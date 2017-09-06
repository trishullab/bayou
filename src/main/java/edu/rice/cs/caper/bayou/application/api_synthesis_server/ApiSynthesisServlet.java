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
package edu.rice.cs.caper.bayou.application.api_synthesis_server;

import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis.*;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging.SynthesisLoggerS3;
import edu.rice.cs.caper.bayou.core.synthesizer.ParseException;
import edu.rice.cs.caper.servlet.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A servlet for accepting api synthesis requests and returning the results produced by an alternate network endpoint.
 */
public class ApiSynthesisServlet extends SizeConstrainedPostBodyServlet implements JsonResponseServlet, CorsAwareServlet
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisServlet.class.getName());

    /**
     * Thread pool for sending synth log results to S3.
     */
    private static final ExecutorService _synthesisLoggerThreadPool = Executors.newFixedThreadPool(1);

    /**
     * The maximum input size of the request body this servlet will allow.
     * // TODO: have a value in config distinct from code completion
     */
    private static int API_SYNTH_MAX_REQUEST_BODY_SIZE_BYTES = Configuration.CodeCompletionRequestBodyMaxBytesCount;

    /**
     * An object for setting the response CORS headers in accordance with the configuration specified
     * allowed origins.
     */
    private CorsHeaderSetter _corsHeaderSetter = new CorsHeaderSetter(Configuration.CorsAllowedOrigins);

    /**
     * How requests should be fulfilled.
     */
    private final ApiSynthesizer _synthesisRequestProcessor;

    private final String _synthesisLogBucketName = Configuration.SynthesisLogBucketName;

    /**
     * Public for reflective construction by Jetty.
     */
    public ApiSynthesisServlet()
    {
        super(API_SYNTH_MAX_REQUEST_BODY_SIZE_BYTES, false);

        ApiSynthesizer synthesisStrategy;
        if(Configuration.UseSynthesizeEchoMode)
        {
            synthesisStrategy =  new ApiSynthesizerEcho(Configuration.EchoModeDelayMs);
        }
        else
        {
            synthesisStrategy = new ApiSynthesizerRemoteTensorFlowAsts("localhost", 8084,
                    Configuration.SynthesizeTimeoutMs,
                    Configuration.EvidenceClasspath,
                    Configuration.AndroidJarPath);
        }

        _synthesisRequestProcessor = new ApiSynthesizerRewriteEvidenceDecorator(synthesisStrategy);


        _logger.debug("exiting");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, String body) throws IOException
    {
        _logger.debug("entering");
        try
        {
            if(req == null) throw new NullPointerException("req");
            if(resp == null) throw new NullPointerException("resp");
            if(body == null) throw new NullPointerException("body");

            doPostHelp(req, resp, body);
        }
        catch (Throwable e)
        {
            _logger.error(e.getMessage(), e);
            JSONObject responseBody = new ErrorJsonResponse("Unexpected exception during synthesis.");
            if(resp != null)
                resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            writeObjectToServletOutputStream(responseBody, resp);
        }
        finally
        {
            _logger.debug("exiting");
        }
    }

    private void doPostHelp(HttpServletRequest req, HttpServletResponse resp, String body)
            throws IOException, SynthesiseException
    {
        _logger.debug("entering");
        _logger.trace("body:" + body);

        if(req == null) throw new NullPointerException("req");
        if(resp == null) throw new NullPointerException("resp");
        if(body == null) throw new NullPointerException("body");

       /*
        * Check that the request comes from a valid origin.  Add appropriate CORS response headers if so.
        */
        boolean requestFromAllowedOrigin = this.applyAccessControlHeaderIfAllowed(req, resp, _corsHeaderSetter);

        if(!requestFromAllowedOrigin)
        {
            _logger.debug("exiting");
            return;
        }

        /*
         * Assign a unique request id to the requests.
         */
        UUID requestId = UUID.randomUUID();
        _logger.info(requestId + ": api synth request");
        _logger.trace(requestId + ": api synth request body " + body);

        /*
         * Parse message into a JSON object.
         */
        JSONObject jsonMessage;
        try
        {
            jsonMessage = new JSONObject(body);
        }
        catch (JSONException e)
        {
            _logger.error(requestId, e);
            JSONObject responseBody = new ErrorJsonResponse("Request body not JSON");
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            writeObjectToServletOutputStream(responseBody, resp);
            _logger.debug("exiting");
            return;
        }

        /*
         * Extract the request code from the JSON message.
         */
        String code;
        {
            final String CODE = "code";
            if (!jsonMessage.has(CODE))
            {
                _logger.warn(requestId + ": JSON message has no " + CODE + " field.");
                JSONObject responseBody = new ErrorJsonResponse("Missing parameter " + CODE);
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                writeObjectToServletOutputStream(responseBody, resp);
                _logger.debug("exiting");
                return;
            }
            code = jsonMessage.get(CODE).toString();

        }

        /*
         * Extract max program count from the JSON message.
         */
        int maxProgamCount;
        {
            final String MAX_PROGRAM_COUNT = "max program count";
            if (!jsonMessage.has(MAX_PROGRAM_COUNT))
            {
                _logger.warn(requestId + ": JSON message has no " + MAX_PROGRAM_COUNT + " field.");
                JSONObject responseBody = new ErrorJsonResponse("Missing parameter " + MAX_PROGRAM_COUNT);
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                writeObjectToServletOutputStream(responseBody, resp);
                _logger.debug("exiting");
                return;
            }

            try
            {
                maxProgamCount = jsonMessage.getInt(MAX_PROGRAM_COUNT);
            }
            catch (JSONException e)
            {
                _logger.warn(requestId + ": JSON message has non-int " + MAX_PROGRAM_COUNT + " field.");
                JSONObject responseBody = new ErrorJsonResponse("Parameter " + MAX_PROGRAM_COUNT + " is not an int.");
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                writeObjectToServletOutputStream(responseBody, resp);
                _logger.debug("exiting");
                return;
            }

        }

        /*
         * Extract sample count from request if present
         */
        Integer sampleCount;
        {
            final String SAMPLE_COUNT = "sample count";
            if (jsonMessage.has(SAMPLE_COUNT))
                sampleCount = jsonMessage.getInt(SAMPLE_COUNT);
            else
                sampleCount = null;
        }

        /*
         * Perform synthesis.
         */
        Iterable<String> results;
        try
        {
            if(sampleCount != null)
                results = _synthesisRequestProcessor.synthesise(code, maxProgamCount, sampleCount);
            else
                results = _synthesisRequestProcessor.synthesise(code, maxProgamCount);
        }
        catch (SynthesiseException e)
        {
            JSONObject responseBody = new ErrorJsonResponse("parseException");
            responseBody.put("exceptionMessage", e.getMessage());
            resp.setStatus(HttpStatus.BAD_REQUEST_400);
            writeObjectToServletOutputStream(responseBody, resp);
            _logger.debug("exiting");
            return;
        }

        /*
         * Place synthesis results into a JSON string, send response, and close socket.
         */
        SuccessJsonResponse responseBody = new SuccessJsonResponse();
        responseBody.put("requestId", requestId.toString());
        LinkedList<String> resultsCollection = new LinkedList<>(); // JSONArray ctor will not accept an Iterable.
        for(String result : results)                               // Use Collection to avoid RuntimeException in ctr.
            resultsCollection.add(result);
        responseBody.put("results", new JSONArray(resultsCollection));
        writeObjectToServletOutputStream(responseBody, resp);

        /*
         * Log result to S3.  Do in other thread so that this thread becomes available again to process client requests.
         */
        _synthesisLoggerThreadPool.submit(
                () -> new SynthesisLoggerS3(_synthesisLogBucketName).log(requestId, code, results));

        _logger.debug("exiting");

    }


}

