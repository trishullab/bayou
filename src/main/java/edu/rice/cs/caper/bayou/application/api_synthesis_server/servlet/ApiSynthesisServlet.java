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
package edu.rice.cs.caper.bayou.application.api_synthesis_server.servlet;

import edu.rice.cs.caper.bayou.application.api_synthesis_server.Configuration;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis.*;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging.SynthesisLoggerNone;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging.SynthesisLoggerS3;
import edu.rice.cs.caper.programming.numbers.NatNum32;
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
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A servlet for accepting api synthesis requests and returning synthesis results.
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
    private static NatNum32 API_SYNTH_MAX_REQUEST_BODY_SIZE_BYTES = Configuration.CodeCompletionRequestBodyMaxBytesCount;

    /*
     * The maximum number of concurrent POST requests that should be allowed among all instances of this servlet.
     */
    private static final NatNum32 OUTSTANDING_POST_REQUEST_COUNT_LIMIT = Configuration.OutstandingSynthRequestCountLimit;

    /*
     * The current number of POST requests being processed among all instances of the servlet.
     */
    private static final AtomicInteger _outstandingPostRequestsCount = new AtomicInteger(0);

    /**
     * An object for setting the response CORS headers in accordance with the configuration specified
     * allowed origins.
     */
    private CorsHeaderSetter _corsHeaderSetter = new CorsHeaderSetter(Configuration.CorsAllowedOrigins);

    /**
     * How requests should be fulfilled.
     */
    private final ApiSynthesizer _synthesisRequestProcessor;

    /*
     * The name of the S3 bucket in which to store synth log messages.
     */
    private final String _synthesisLogBucketName = Configuration.SynthesisLogBucketName;

    /**
     * Public for reflective construction by Jetty.
     */
    public ApiSynthesisServlet()
    {
        super(API_SYNTH_MAX_REQUEST_BODY_SIZE_BYTES, false);

        ApiSynthesizer synthesisStrategy = ApiSynthesizerFactory.makeFromConfig(true);
        _synthesisRequestProcessor = new ApiSynthesizerRewriteEvidenceDecorator(synthesisStrategy);
        _logger.debug("exiting");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, String requestBody) throws IOException
    {
        _logger.debug("entering");
        try
        {
            int outstandingRequestsCount = _outstandingPostRequestsCount.incrementAndGet();
            try
            {
                _logger.trace("Outstanding request count = " + _outstandingPostRequestsCount);
                // check if fulfilling this request would put us over the concurrent synth
                // request processing limit.
                if (outstandingRequestsCount > OUTSTANDING_POST_REQUEST_COUNT_LIMIT.AsInt)
                {
                    _logger.warn("Returning 429: too many requests");
                    JSONObject responseBody = new ErrorJsonResponse("Too many requests.");
                    resp.setStatus(HttpStatus.TOO_MANY_REQUESTS_429);
                    writeObjectToServletOutputStream(responseBody, resp);
                    return;
                }

                doPostHelp(req, resp, requestBody);
            }
            finally
            {
                _outstandingPostRequestsCount.decrementAndGet();
                _logger.trace("Decrement");
            }
        }
        catch (Throwable e)
        {
            _logger.error(e.getMessage(), e);
            JSONObject responseBody = new ErrorJsonResponse("Unexpected exception during synthesis.");
            resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            writeObjectToServletOutputStream(responseBody, resp);
        }
        finally
        {
            _logger.debug("exiting");
        }
    }

    private void doPostHelp(HttpServletRequest req, HttpServletResponse resp, String requestBody) throws IOException
    {
        _logger.debug("entering");
        _logger.info("api synthesis request");

        /*
         * Check inputs.
         */
        if(req == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("req");
        }

        if(resp == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("resp");
        }

        if(requestBody == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("requestBody");
        }

       /*
        * Check that the request comes from a valid origin.  Add appropriate CORS response headers if so.
        */
        boolean requestFromAllowedOrigin = this.applyAccessControlHeaderIfAllowed(req, resp, _corsHeaderSetter);

        if(!requestFromAllowedOrigin)
        {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            _logger.debug("exiting");
            return;
        }

        /*
         * Assign a unique request id to the requests.
         */
        UUID requestId = UUID.randomUUID();
        _logger.info(requestId + ": api synth request");
        _logger.trace(requestId + ": api synth request body " + requestBody);

        /*
         * Parse message body into a SynthesisRequest.
         */
        SynthesisRequest requestMsg;
        {
            JSONObject jsonMessage;
            try
            {
                jsonMessage = new JSONObject(requestBody);
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

            final String MAX_PROGRAM_COUNT = "max program count";

            try
            {
                requestMsg = SynthesisRequest.make(jsonMessage);
            }
            catch (SynthesisRequest.NoCodeFieldException e)
            {
                final String CODE = "code";
                _logger.warn(requestId + ": message has no " + CODE + " field.");
                JSONObject responseBody = new ErrorJsonResponse("Missing parameter " + CODE);
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                writeObjectToServletOutputStream(responseBody, resp);
                _logger.debug("exiting");
                return;
            }
            catch (SynthesisRequest.NoMaxProgramCountFieldException e)
            {
                _logger.warn(requestId + ": JSON message has no " + MAX_PROGRAM_COUNT + " field.");
                JSONObject responseBody = new ErrorJsonResponse("Missing parameter " + MAX_PROGRAM_COUNT);
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                writeObjectToServletOutputStream(responseBody, resp);
                _logger.debug("exiting");
                return;
            }
            catch (SynthesisRequest.InvalidMaxProgramCountException e)
            {
                _logger.warn(requestId + ": JSON message has invalid " + MAX_PROGRAM_COUNT + " field.");
                JSONObject responseBody = new ErrorJsonResponse("Bad parameter " + MAX_PROGRAM_COUNT);
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                writeObjectToServletOutputStream(responseBody, resp);
                _logger.debug("exiting");
                return;
            }
            catch (SynthesisRequest.InvalidSampleCountException e)
            {
                final String SAMPLE_COUNT = "sample count";
                _logger.warn(requestId + ": JSON message has invalid " + SAMPLE_COUNT + " field.");
                JSONObject responseBody = new ErrorJsonResponse("Bad parameter " + SAMPLE_COUNT);
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                writeObjectToServletOutputStream(responseBody, resp);
                _logger.debug("exiting");
                return;
            }
        }

        /*
         * Process request in terms of SynthesisRequest.
         */
        doPostHelp(resp, requestId, requestMsg);

        _logger.debug("exiting");

    }

    // isRequestLocal indicates if the origin of the request is the same machine as the http server.
    private void doPostHelp(HttpServletResponse resp, UUID requestId, SynthesisRequest requestMsg) throws IOException
    {
        _logger.debug("entering");

        String code = requestMsg.getCode();

        /*
         * Perform synthesis.
         */
        Iterable<String> results;
        try
        {
            results = _synthesisRequestProcessor.synthesise(code, requestMsg.getMaxProgramCount());
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
         * Place synthesis results into a JSON string and send response.
         */
        SuccessJsonResponse responseBody = new SuccessJsonResponse();
        responseBody.put("requestId", requestId.toString());
        LinkedList<String> resultsCollection = new LinkedList<>(); // JSONArray ctor will not accept an Iterable.
        for(String result : results)                               // Use Collection to avoid RuntimeException in ctr.
            resultsCollection.add(result);
        responseBody.put("results", new JSONArray(resultsCollection));
        writeObjectToServletOutputStream(responseBody, resp);

        /*
         * Log result.  Do in other thread so that this thread becomes available again to process client requests.
         */
        _synthesisLoggerThreadPool.submit(
                () -> new SynthesisLoggerNone().log(requestId, code, results));

        _logger.debug("exiting");
    }
}

