package edu.rice.cs.caper.bayou.application.api_synthesis_server;

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
import java.util.function.Supplier;

/**
 * A servlet for accepting api synthesis requests and returning the results produced by an alternate network endpoint.
 */
public class ApiSynthesisServlet extends SizeConstrainedPostBodyServlet implements JsonResponseServlet
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisServlet.class.getName());

    /**
     * The maximum input size of the request body this servlet will allow.
     * // TODO: have a value in config distinct from code completion
     */
    private static int API_SYNTH_MAX_REQUEST_BODY_SIZE_BYTES = Configuration.CodeCompletionRequestBodyMaxBytesCount;

    /**
     * An object for setting the response CORS headers in accordance with the configuration specified
     * allowed origins.
     */
    private static final CorsHeaderSetter _corsHeaderSetter = new CorsHeaderSetter(Configuration.CorsAllowedOrigins);

    /**
     * How requests should be fulfilled.
     */
    private final ApiSynthesisStrategy _synthesisStrategy = ApiSynthesisStrategy.fromConfig();

    /**
     * Public for reflective construction by Jetty.
     */
    public ApiSynthesisServlet()
    {
        super(((Supplier<Integer>)() -> { _logger.debug("entering ApiSynthesisServlet");
                                         return API_SYNTH_MAX_REQUEST_BODY_SIZE_BYTES; }).get(),
                false);

        _logger.debug("exiting");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, String body) throws IOException
    {
        _logger.debug("entering");
        try
        {
            doPostHelp(req, resp, body);
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

    private void doPostHelp(HttpServletRequest req, HttpServletResponse resp, String body)
            throws IOException, ApiSynthesisStrategy.SynthesiseException
    {
        _logger.debug("entering");

        // only allow CORS processing from origins we approve
        boolean requestFromAllowedOrigin = _corsHeaderSetter.applyAccessControlHeaderIfAllowed(req, resp);

        if(!requestFromAllowedOrigin)
        {
            resp.setStatus(HttpStatus.FORBIDDEN_403);
            _logger.warn("request from unauthorized origin.");
            _logger.debug("exiting");
            return;
        }

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
         * Access the request code from the JSON message.
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
         * Perform synthesis.
         */
        Iterable<String> results = _synthesisStrategy.synthesise(code);

        /*
         * Place synthesis results into a JSON string, send response, and close socket.
         */
        SuccessJsonResponse responseBody = new SuccessJsonResponse();
        LinkedList<String> resultsCollection = new LinkedList<>(); // JSONArray ctor will not accept an Iterable.
        for(String result : results)                               // Use Collection to avoid RuntimeException in ctr.
            resultsCollection.add(result);
        responseBody.put("results", new JSONArray(resultsCollection));
        writeObjectToServletOutputStream(responseBody, resp);
        _logger.debug("exiting");
    }


}

