package edu.rice.cs.caper.bayou.application.api_synthesis_server;

import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging.SynthesisQualityFeedbackLogger;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging.SynthesisQualityFeedbackLoggerS3;
import edu.rice.cs.caper.servlet.JsonResponseServlet;
import edu.rice.cs.caper.servlet.SizeConstrainedPostBodyServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * A servlet for accepting user feedback on synthesis result quality.
 */
public class ApiSynthesisResultQualityFeedbackServlet extends SizeConstrainedPostBodyServlet
                                                      implements JsonResponseServlet
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisResultQualityFeedbackServlet.class.getName());

    /**
     * The place to store user feedback.
     */
    private final SynthesisQualityFeedbackLogger _feedbackLogger
            = new SynthesisQualityFeedbackLoggerS3(Configuration.SynthesisQualityFeedbackLogBucketName);

    // public no-arg constructor for reflective construction by Jetty.
    public ApiSynthesisResultQualityFeedbackServlet()
    {
        super(Configuration.CodeCompletionRequestBodyMaxBytesCount, false);
        _logger.debug("exiting");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp, String body) throws IOException
    {
        _logger.debug("entering");
        decodeBodyAndLog(body, _feedbackLogger);
        _logger.debug("exiting");
    }

    static void decodeBodyAndLog(String body, SynthesisQualityFeedbackLogger logger)
    {
        _logger.debug("entering");

        if(body == null)
        {
            _logger.debug("exiting");
            throw new NullPointerException("body");
        }

        JSONObject bodyJson = new JSONObject(body);

        UUID requestId = UUID.fromString(bodyJson.getString("requestId"));
        String searchCode = bodyJson.getString("searchCode");
        String resultCode = bodyJson.getString("resultCode");
        boolean isGood = bodyJson.getBoolean("isGood");

        logger.log(requestId, searchCode, resultCode, isGood);

        _logger.debug("exiting");
    }
}
