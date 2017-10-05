/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;

import edu.rice.cs.caper.bayou.core.synthesizer.EvidenceExtractor;
import edu.rice.cs.caper.bayou.core.synthesizer.ParseException;
import edu.rice.cs.caper.bayou.core.synthesizer.Parser;
import edu.rice.cs.caper.bayou.core.synthesizer.Synthesizer;
import edu.rice.cs.caper.programming.ContentString;
import edu.rice.cs.caper.programming.numbers.NatNum32;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;

/**
 * A synthesis strategy that relies on a remote server that uses TensorFlow to create ASTs from extracted evidence.
 */
public class ApiSynthesizerRemoteTensorFlowAsts implements ApiSynthesizer
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesizerRemoteTensorFlowAsts.class.getName());

    /**
     * A pool wrapping a single thread used to perform the work of sending requests to the remote server.
     * Work done in separate thread so we can issue a timeout for the whole operation as per _maxNetworkWaitTimeMs.
     */
    private final ExecutorService _sendRequestPool = Executors.newFixedThreadPool(1);

    /**
     * The network name of the tensor flow host server.
     */
    private final ContentString _tensorFlowHost;

    /**
     * The port the tensor flow host server on which connections requests are expected.
     */
    private final NatNum32 _tensorFlowPort;

    /**
     * The max time in milliseconds to wait for a response from the remote server.
     */
    private final NatNum32 _maxNetworkWaitTimeMs;

    /**
     * A classpath string that includes the class edu.rice.cs.caper.bayou.annotations.Evidence.
     */
    private final ContentString _evidenceClasspath;

    /**
     * The path to android.jar.
     */
    private final File _androidJarPath;

    /**
     * @param tensorFlowHost  The network name of the tensor flow host server. May not be null.
     * @param tensorFlowPort The port the tensor flow host server on which connections requests are expected.
     *                       May not be negative.
     * @param maxNetworkWaitTimeMs The max time in milliseconds to wait for a response from the tensor flow host.
     * @param evidenceClasspath A classpath string that includes the class edu.rice.cs.caper.bayou.annotations.Evidence.
     *                          May not be null.
     * @param androidJarPath The path to android.jar. May not be null.
     */
    public ApiSynthesizerRemoteTensorFlowAsts(ContentString tensorFlowHost, NatNum32 tensorFlowPort,
                                              NatNum32 maxNetworkWaitTimeMs, ContentString evidenceClasspath,
                                              File androidJarPath)
    {
        _logger.debug("entering");

        _androidJarPath = androidJarPath;

        if(tensorFlowHost == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("tensorFlowHost");
        }

        if(evidenceClasspath == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("evidenceClasspath");
        }

        if(androidJarPath == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("androidJarPath");
        }

        if(!androidJarPath.exists())
        {
            _logger.debug("exiting");
            throw new IllegalArgumentException("androidJarPath does not exist: " + androidJarPath.getAbsolutePath());
        }

        _tensorFlowHost = tensorFlowHost;
	    _logger.trace("_tensorFlowHost:" + _tensorFlowHost);
        _tensorFlowPort = tensorFlowPort;
        _maxNetworkWaitTimeMs = maxNetworkWaitTimeMs;
        _evidenceClasspath = evidenceClasspath;
        _logger.trace("_evidenceClasspath:" + _evidenceClasspath);
        _logger.debug("exiting");
    }

    @Override
    public Iterable<String> synthesise(String searchCode, NatNum32 maxProgramCount) throws SynthesiseException
    {
        return synthesiseHelp(searchCode, maxProgramCount, null);
    }

    @Override
    public Iterable<String> synthesise(String searchCode, NatNum32 maxProgramCount, NatNum32 sampleCount)
            throws SynthesiseException
    {
        _logger.debug("entering");

        if(sampleCount == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("sampleCount");
        }

        return synthesiseHelp(searchCode, maxProgramCount, sampleCount);
    }

    // sampleCount of null means don't send a sampleCount key in the request message.  Means use default sample count.
    private Iterable<String> synthesiseHelp(String code, NatNum32 maxProgramCount, NatNum32 sampleCount)
            throws SynthesiseException
    {
        _logger.debug("entering");

        /*
         * Check parameters.
         */
        if(maxProgramCount == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("maxProgramCount");
        }

        if(code == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("code");
        }

        /*
         * Parse the program.
         */
        Parser parser;
        try
        {
            String combinedClassPath = _evidenceClasspath + File.pathSeparator + _androidJarPath.getAbsolutePath();
            parser = new Parser(code, combinedClassPath);
            parser.parse();
        }
        catch (ParseException e)
        {
            throw new SynthesiseException(e);
        }

        /*
         * Extract a description of the evidence in the search code that should guide AST results generation.
         */
        String evidence;
        try
        {
            evidence = extractEvidence(new EvidenceExtractor(), parser);
        }
        catch (ParseException e)
        {
            throw new SynthesiseException(e);
        }

        /*
         * Contact the remote Python server and provide evidence to be fed to Tensor Flow to generate solution
         * ASTs.
         */
        String astsJson = sendGenerateAstRequest(evidence, maxProgramCount, sampleCount);
        _logger.trace("astsJson:" + astsJson);

        /*
         * Synthesise results from the code and asts and return.
         */
        List<String> synthesizedPrograms;
        synthesizedPrograms = new Synthesizer().execute(parser, astsJson);

        if(synthesizedPrograms.size() > maxProgramCount.AsInt) // unsure if execute always returns n output for n ast inputs.
            synthesizedPrograms = synthesizedPrograms.subList(0, maxProgramCount.AsInt);

        _logger.trace("synthesizedPrograms: " + synthesizedPrograms);
        _logger.debug("exiting");
        return synthesizedPrograms;
    }

    // sampleCount of null means don't send a sampleCount key in the request message.  Means use default sample count.
    private String sendGenerateAstRequest(String evidence, NatNum32 maxProgramCount, NatNum32 sampleCount)
            throws SynthesiseException
    {
        _logger.debug("entering");

        /*
         * Check parameters.
         */
        if(evidence == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("evidence");
        }

        if(maxProgramCount == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("maxProgramCount");
        }

        /*
         * Send request to the server asynchronously so we can wait a defined time for a response.
         */
        Future<String> responseBody = _sendRequestPool.submit(() ->
        {
           RequestConfig config = RequestConfig.custom().setConnectTimeout(_maxNetworkWaitTimeMs.AsInt)
                                                        .setSocketTimeout(_maxNetworkWaitTimeMs.AsInt).build();

           try(CloseableHttpClient httpclient = HttpClientBuilder.create().setDefaultRequestConfig(config).build())
           {
               JSONObject requestObj = new JSONObject();
               requestObj.put("request type", "generate asts");
               requestObj.put("evidence", evidence);
               requestObj.put("max ast count", maxProgramCount.AsInt);

               if(sampleCount != null)
                   requestObj.put("sample count", sampleCount);

               HttpPost httppost = new HttpPost("http://" + _tensorFlowHost + ":" + _tensorFlowPort);
               httppost.setEntity(new StringEntity(requestObj.toString(2)));

               HttpResponse response = httpclient.execute(httppost);
               int responseStatusCode = response.getStatusLine().getStatusCode();

               if (responseStatusCode != 200)
                   throw new IOException("Unexpected http response code: " + responseStatusCode);

               HttpEntity entity = response.getEntity();

               if (entity == null)
                   throw new IOException("Expected response body.");


               return IOUtils.toString(entity.getContent(), "UTF-8");
           }
        });

        /*
         * Wait at most _maxNetworkWaitTimeMs ms for response and return response (or exception on timeout).
         */
        try
        {
            String value = responseBody.get((long)_maxNetworkWaitTimeMs.AsInt, TimeUnit.MILLISECONDS);
            _logger.debug("exiting");
            return value;
        }
        catch (InterruptedException | ExecutionException | TimeoutException  e)
        {
            _logger.debug("exiting");
            throw new SynthesiseException(e);
        }

    }

    /**
     * @return extractor.execute(code, evidenceClasspath) if value is non-null.
     * @throws SynthesiseException if extractor.execute(code, evidenceClasspath) is null
     */
    private static String extractEvidence(EvidenceExtractor extractor, Parser parser)
            throws SynthesiseException, ParseException
    {
        _logger.debug("entering");
        String evidence = extractor.execute(parser);
        if(evidence == null)
        {
            _logger.debug("exiting");
            throw new SynthesiseException("evidence may not be null.  Input was " +
                    parser.getSource());
        }
        _logger.trace("evidence:" + evidence);
        _logger.debug("exiting");
        return evidence;
    }
}
