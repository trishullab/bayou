package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging;

import com.amazonaws.auth.AWSCredentials;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import static edu.rice.cs.caper.programming.Assertions.*;

/**
 * A SynthesisQualityFeedbackLogger that stores the log message on AWS S3.
 * Stores log messages as a JSON string using UTF-8 encoding.
 */
public class SynthesisQualityFeedbackLoggerS3 extends S3LoggerBase implements SynthesisQualityFeedbackLogger
{
    /**
     * Place to send application logging information.
     */
    private static final Logger _logger = LogManager.getLogger(SynthesisQualityFeedbackLoggerS3.class.getName());

    /**
     * Will use environment credentials when authenticating with S3.
     *
     * @param bucketName The name of the S3 bucket where log message should be stored. May not be null, white space
     *                   only, or empty.
     */
    public SynthesisQualityFeedbackLoggerS3(String bucketName)
    {
        super(null, bucketName);
    }

    /**
     * @param bucketName The name of the S3 bucket where log message should be stored. May not be null, white space
     *                   only, or empty.
     * @param creds The credentials to be used for communicating with S3. null indicates the environment's credentials
     *              should be used.
     */
    public SynthesisQualityFeedbackLoggerS3(String bucketName, AWSCredentials creds)
    {
        super(assertArgumentNonNull("creds", creds), bucketName);
    }


    @Override
    public void log(UUID requestId, String searchCode, String resultCode, boolean isGood)
    {
        _logger.debug("entering");

        /*
         * Make the log message to send to S3.
         */
        String logMsg;
        {
            long now = System.currentTimeMillis();

            JSONObject recordObj = new JSONObject();
            recordObj.put("version", "1");
            recordObj.put("requestId", requestId);
            recordObj.put("searchCode", searchCode);
            recordObj.put("resultCode", resultCode);
            recordObj.put("isGood", isGood);
            recordObj.put("logMomentUtc", now);
            recordObj.put("logMomentHuman", DateFormat.getDateTimeInstance().format(new Date(now)));

            logMsg = recordObj.toString(1);
        }

        String objectKey = requestId.toString() + ".txt";
        this.putToS3(objectKey, logMsg);

        _logger.debug("exiting");
    }
}
