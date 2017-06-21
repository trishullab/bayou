package edu.rice.cs.caper.bayou.program.api_synthesis_server.synthesis_logging;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * A SynthesisLogger that stores the log message on AWS S3.  Stores log messages as a JSON string using UTF-8 encoding.
 */
public class SynthesisLoggerS3 implements SynthesisLogger
{
    /**
     * Place to send program logging information.
     */
    private static final Logger _logger = LogManager.getLogger(SynthesisLoggerS3.class.getName());

    private static <T> T assertNonNull(String paramName, T value)
    {
        _logger.debug("entering");
        try
        {
            if (value == null)
                throw new NullPointerException(paramName);

            return value;
        }
        finally
        {
            _logger.debug("exiting");
        }
    }

    /**
     * The credentials to be used for communicating with S3.  null indicates that the environment's creds should
     * be used instead of explicit creds.
     */
    private final AWSCredentials _credentials;

    /**
     * The name of the S3 bucket where log message should be stored. May not be null, white space only, or empty.
     */
    private final String _bucketName;

    /**
     * Will use environment credentials when authenticating with S3.
     *
     * @param bucketName The name of the S3 bucket where log message should be stored. May not be null, white space
     *                   only, or empty.
     */
    public SynthesisLoggerS3(String bucketName)
    {
        this(bucketName, null, null);
    }

    /**
     * @param bucketName The name of the S3 bucket where log message should be stored. May not be null, white space
     *                   only, or empty.
     * @param creds The credentials to be used for communicating with S3. null indicates the environment's credentials
     *              should be used.
     */
    public SynthesisLoggerS3(String bucketName, AWSCredentials creds)
    {
        this(bucketName, assertNonNull("creds", creds), null);
    }

    // ignore param is just so that our signature does not collide with the public version of the constructor.
    // we can't use a non-constructor helper method for this method instead beacuse we are setting final fields.
    private SynthesisLoggerS3(String bucketName, AWSCredentials creds, Void ignore)
    {
        _logger.debug("entering");

        if(bucketName == null)
            throw new NullPointerException("bucketName may not be null");

        if(bucketName.trim().length() == 0)
            throw new IllegalArgumentException("bucketName must have at least one non-whitespace character");

        _credentials = creds;
        _bucketName = bucketName;

        _logger.debug("exiting");
    }

    @Override
    public void log(UUID requestId, String searchCode, List<String> results)
    {
        _logger.debug("entering");

        /*
         * Make the log message to send to S3.
         */
        String logMsg;
        {
            long now = System.currentTimeMillis();

            JSONObject recordObj = new JSONObject();
            recordObj.put("requestId", requestId);
            recordObj.put("searchCode", searchCode);
            recordObj.put("logMomentUtc", now);
            recordObj.put("logMomentHuman", DateFormat.getDateTimeInstance().format(new Date(now)));

            JSONArray resultsArray = new JSONArray();
            for (String result : results)
            {
                resultsArray.put(result);
            }

            recordObj.put("results", resultsArray);

            logMsg = recordObj.toString(1);
        }

        /*
         * Make the client used to send the log msg to S3.
         */
        AmazonS3 client;
        {
            Regions regions = Regions.US_EAST_1;
            if(_credentials == null)
            {
                client = AmazonS3ClientBuilder.standard() // get creds from environment
                                              .withRegion(regions)
                                              .build();
            }
            else
            {
                client =  AmazonS3ClientBuilder.standard()
                                               .withCredentials(new AWSStaticCredentialsProvider(_credentials))
                                               .withRegion(regions)
                                               .build();
            }
        }


        /*
         * Store the log msg in S3.
         */
        byte[] logMsgBytes = logMsg.getBytes(StandardCharsets.UTF_8);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(logMsgBytes.length);

        String objectKey = requestId.toString() + ".txt";
        client.putObject(_bucketName, objectKey, new ByteArrayInputStream(logMsgBytes), metadata);

        _logger.debug("exiting");
    }
}
