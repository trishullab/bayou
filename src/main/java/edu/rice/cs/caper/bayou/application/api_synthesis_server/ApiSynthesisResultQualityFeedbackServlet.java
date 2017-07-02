package edu.rice.cs.caper.bayou.application.api_synthesis_server;

import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging.SynthesisQualityFeedbackLogger;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging.SynthesisQualityFeedbackLoggerS3;
import edu.rice.cs.caper.servlet.CorsHeaderSetter;
import edu.rice.cs.caper.servlet.ErrorJsonResponse;
import edu.rice.cs.caper.servlet.JsonResponseServlet;
import edu.rice.cs.caper.servlet.SizeConstrainedPostBodyServlet;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * A servlet for accepting user feedback on synthesis result quality.
 */
public class ApiSynthesisResultQualityFeedbackServlet extends SizeConstrainedPostBodyServlet
                                                      implements JsonResponseServlet, CorsAwareServlet

{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger =
            LogManager.getLogger(ApiSynthesisResultQualityFeedbackServlet.class.getName());
    
    /**
     * The place to store user feedback.
     */
    private final SynthesisQualityFeedbackLogger _feedbackLogger
            = new SynthesisQualityFeedbackLoggerS3(Configuration.SynthesisQualityFeedbackLogBucketName);


    /**
     * An object for setting the response CORS headers in accordance with the configuration specified
     * allowed origins.
     */
    private CorsHeaderSetter _corsHeaderSetter = new CorsHeaderSetter(Configuration.CorsAllowedOrigins);


    // public no-arg constructor for reflective construction by Jetty.
    public ApiSynthesisResultQualityFeedbackServlet()
    {
        super(Configuration.CodeCompletionRequestBodyMaxBytesCount, false);
        _logger.debug("exiting");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, String body) throws IOException
    {
        try
        {
            if(req == null) throw new NullPointerException("req");
            if(resp == null) throw new NullPointerException("resp");
            if(body == null) throw new NullPointerException("body");

            _logger.debug("entering");

            /*
             * Check that the request comes from a valid origin.  Add appropriate CORS response headers if so.
             */
            boolean allowedOrigin = this.applyAccessControlHeaderIfAllowed(req, resp, _corsHeaderSetter);
            if(!allowedOrigin)
            {
                _logger.debug("exiting");
                return;
            }

            /*
             * Record feedback.
             */
            JSONObject jsonMessage;
            try
            {
                jsonMessage = new JSONObject(body);
            }
            catch (JSONException e)
            {
                JSONObject responseBody = new ErrorJsonResponse("Request body not JSON");
                resp.setStatus(HttpStatus.BAD_REQUEST_400);
                writeObjectToServletOutputStream(responseBody, resp);
                _logger.debug("exiting");
                return;
            }
            decodeBodyAndLog(jsonMessage, _feedbackLogger);

        }
        catch (Throwable e)
        {
            _logger.error(e.getMessage(), e);
            if(resp != null)
                resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR_500);
            JSONObject responseBody = new ErrorJsonResponse("Unexpected exception during feedback.");
            writeObjectToServletOutputStream(responseBody, resp);
        }
        finally
        {
            _logger.debug("exiting");
        }
    }

    // static for no-construction unit testing
    static void decodeBodyAndLog(JSONObject body, SynthesisQualityFeedbackLogger logger)
    {
        _logger.debug("entering");

        if(body == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("body");
        }

        UUID requestId = UUID.fromString(body.getString("requestId"));
        String searchCode = body.getString("searchCode");
        String resultCode = body.getString("resultCode");
        boolean isGood = body.getBoolean("isGood");

        logger.log(requestId, searchCode, resultCode, isGood);

        _logger.debug("exiting");
    }
}
