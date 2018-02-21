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
import edu.rice.cs.caper.programming.thread.Operation;
import edu.rice.cs.caper.programming.thread.ThreadScheduler;
import edu.rice.cs.caper.programming.thread.ThreadSchedulerFifoInterrupt;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

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
     * Thread scheduler to ensure that the TensorFlow synthesizer has 1 client at a time
     * *NOTE*: This restriction is a property of Python being single threaded.
     *         If the synthesizer moves to a multithreaded implementation, the thread scheduler
     *         might be obviated.
     */
    private static final ThreadScheduler _scheduler = new ThreadSchedulerFifoInterrupt(new NatNum32(1));

    /**
     * The network name of the tensor flow host server.
     */
    private final ContentString _tensorFlowHost;

    /**
     * The port of the tensor flow host on which connections requests are expected.
     */
    private final NatNum32 _tensorFlowPort;

    /**
     * The max time in milliseconds to wait for all responses per request from the tensor flow host.
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
     * The type of API synthesis that should be performed.
     */
    private final Synthesizer.Mode _synthMode;

    /**
     * @param tensorFlowHost  The network name of the tensor flow host server. May not be null.
     * @param tensorFlowPort The port the tensor flow host server on which connections requests are expected.
     *                       May not be negative.
     * @param maxNetworkWaitTimeMs The max time in milliseconds to wait for a response from the tensor flow host.
     * @param evidenceClasspath A classpath string that includes the class edu.rice.cs.caper.bayou.annotations.Evidence.
*                          May not be null.
     * @param androidJarPath The path to android.jar. May not be null.
     * @param synthMode The type of API synthesis that should be performed.
     */
    public ApiSynthesizerRemoteTensorFlowAsts(ContentString tensorFlowHost, NatNum32 tensorFlowPort,
                                              NatNum32 maxNetworkWaitTimeMs, ContentString evidenceClasspath,
                                              File androidJarPath, Synthesizer.Mode synthMode)
    {
        _logger.debug("entering");

        if(tensorFlowHost == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("tensorFlowHost");
        }

        if(tensorFlowPort == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("tensorFlowPort");
        }

        if(maxNetworkWaitTimeMs == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("maxNetworkWaitTimeMs");
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

        if(synthMode == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("tensorFlowHost");
        }

        _tensorFlowHost = tensorFlowHost;
        _tensorFlowPort = tensorFlowPort;
        _maxNetworkWaitTimeMs = maxNetworkWaitTimeMs;
        _evidenceClasspath = evidenceClasspath;
        _androidJarPath = androidJarPath;
        _synthMode = synthMode;

	    _logger.trace("_tensorFlowHost:" + _tensorFlowHost);
        _logger.trace("_evidenceClasspath:" + _evidenceClasspath);
        _logger.debug("exiting");
    }

    @Override
    public Iterable<String> synthesise(String searchCode, NatNum32 maxProgramCount) throws SynthesiseException
    {
        return synthesiseHelp(searchCode, maxProgramCount);
    }

    // sampleCount of null means don't send a sampleCount key in the request message.  Means use default sample count
    // server side.
    private Iterable<String> synthesiseHelp(String code, NatNum32 maxProgramCount) throws SynthesiseException
    {
        _logger.debug("entering");

        /*
         * Check parameters.
         */
        if(code == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("code");
        }


        if(maxProgramCount == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("maxProgramCount");
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
         * Contact the remote server (possibly multiple network requests) and provide evidence to be fed to Tensor Flow
         * to generate solution ASTs.
         */
        JSONObject astsJson = null;
        /*
         * For the current implementation of this synthesizer, we must serialize the request to the python layer.
         *
         */
        try
        {
            /*
             * Send sendGenerate... to our thread scheduler
             */
            astsJson = _scheduler.schedule(() -> sendGenerateAstRequest(evidence));
        }
        catch (IOException e)
        {
            throw new SynthesiseException(e);
        }
        _logger.trace("astsJson:" + astsJson);

        /*
         * Synthesise results from the code and asts and return.
         */
        List<String> synthesizedPrograms;
        {
            synthesizedPrograms = new Synthesizer(_synthMode).execute(parser, astsJson.toString());

            if (synthesizedPrograms.size() > maxProgramCount.AsInt) // only return top maxProgramCount
                synthesizedPrograms = synthesizedPrograms.subList(0, maxProgramCount.AsInt);
        }

        _logger.trace("synthesizedPrograms: " + synthesizedPrograms);
        _logger.debug("exiting");
        return synthesizedPrograms;
    }

    private JSONObject sendGenerateAstRequest(String evidence)
            throws IOException
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

        String astServerHttpResponseBody;
        {

            RequestConfig config = RequestConfig.custom().setConnectTimeout(_maxNetworkWaitTimeMs.AsInt)
                    .setSocketTimeout(_maxNetworkWaitTimeMs.AsInt)
                    .build();

            try (CloseableHttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build())
            {
                JSONObject requestObj = new JSONObject();
                requestObj.put("request type", "generate asts");
                requestObj.put("evidence", evidence);

                HttpPost httppost = new HttpPost("http://" + _tensorFlowHost + ":" + _tensorFlowPort);
                httppost.setEntity(new StringEntity(requestObj.toString(2)));

                HttpResponse response = client.execute(httppost);
                int responseStatusCode = response.getStatusLine().getStatusCode();

                if (responseStatusCode != 200)
                    throw new IOException("Unexpected http response code: " + responseStatusCode);

                HttpEntity entity = response.getEntity();

                if (entity == null)
                    throw new IOException("Expected response body.");

                astServerHttpResponseBody = IOUtils.toString(entity.getContent(), "UTF-8");
            }
        }

        JSONObject responseObject = new JSONObject(astServerHttpResponseBody);
        _logger.debug("exiting");
        return responseObject;

    }

    /**
     * Test that the given JSON object has a top level "evidences" member of type JSON object and a top level "asts"
     * member of type JSON array.
     */
    private boolean isWellFormedResponseObject(JSONObject responseObject)
    {
        _logger.debug("entering");

        try
        {
            final String EVIDENCES = "evidences";
            if (!responseObject.has(EVIDENCES))
                return false;

            try
            {
                responseObject.getJSONObject(EVIDENCES);
            }
            catch (JSONException e)
            {
                return false;
            }

            final String ASTS = "asts";
            if (!responseObject.has(ASTS))
                return false;

            try
            {
                responseObject.getJSONArray(ASTS);
            }
            catch (JSONException e)
            {
                return false;
            }

            return true;
        }
        finally
        {
            _logger.debug("exiting");
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
            throw new SynthesiseException("evidence may not be null.  Input was " + parser.getSource());
        }

        _logger.trace("evidence:" + evidence);
        _logger.debug("exiting");
        return evidence;
    }
}
