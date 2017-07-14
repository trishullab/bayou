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
package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/**
 * Option base class for classes that use S3 as their logging destination.
 */
abstract class S3LoggerBase
{
    /**
     * Place to send application logging information.
     */
    private static final Logger _logger = LogManager.getLogger(S3LoggerBase.class.getName());

    /**
     * The credentials to be used for communicating with S3.  null indicates that the environment's creds should
     * be used instead of explicit creds.
     */
    private final AWSCredentials _credentials;

    /**
     * The name of the S3 bucket where log message should be stored. May not be null, white space only, or empty.
     */
    private final String _bucketName;

    // null credentials interpreted to mean use the environment credentials
    S3LoggerBase(AWSCredentials credentials, String bucketName)
    {
        _logger.debug("entering");

        if(bucketName == null)
            throw new NullPointerException("bucketName may not be null");

        if(bucketName.trim().length() == 0)
            throw new IllegalArgumentException("bucketName must have at least one non-whitespace character");

        _credentials = credentials;
        _bucketName = bucketName;
        _logger.debug("exiting");
    }

    /**
     * Performs an S3 Put Object operation storing the UTF-8 bytes of logMsg under the given key
     * using construction provided AWS credentials.
     *
     * @param objectKey the S3 object key. may not be null or whitespace only.
     * @param logMsg the message to store
     * @throws IllegalArgumentException if objectKey is whitespace only.
     */
    void putToS3(String objectKey, String logMsg)
    {
        if(objectKey == null)
            throw new NullPointerException("objectKey");

        if(objectKey.trim().length() == 0)
            throw new IllegalArgumentException("objectKey may not be only whitespace.");

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

        client.putObject(_bucketName, objectKey, new ByteArrayInputStream(logMsgBytes), metadata);

        _logger.debug("exiting");
    }
}
