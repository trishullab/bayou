package edu.rice.cs.caper.bayou.program.api_synthesis_server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * Configuration options for the Api Synthesis Server program.
 */
class Configuration
{
    static final int RequestProcessingThreadPoolSize;

    static final int ListenPort;

    static final int SynthesizeTimeoutMs;

    static final boolean UseSynthesizeEchoMode;

    static final long EchoModeDelayMs;

    static final String SynthesisLogBucketName;

    static final String EvidenceClasspath;

    static final File AndroidJarPath;

    private static final int MEGA_BYTES_IN_BYTES = 1000000;

    static int CodeCompletionRequestBodyMaxBytesCount = MEGA_BYTES_IN_BYTES;

    static final String[] CorsAllowedOrigins = new String[] { "http://www.askbayou.com", "http://askbayou.com" };

    static
    {
        Properties properties = new Properties();
        {
            String propertyKey = "configurationFile";

            String configPathStr = System.getProperty(propertyKey) != null ? System.getProperty(propertyKey) :
                    "apiSynthesisServerConfig.properties";
            File configPath;
            try
            {
                // do getCanonicalFile to resolve path entries like ../
                // this makes for better error messages in the RuntimeException created if .load(...) exceptions.
                configPath = new File(configPathStr).getCanonicalFile();
            }
            catch (IOException e)
            {
                throw new RuntimeException("Could not load configuration file: " + configPathStr, e);
            }

            try
            {
                properties.load(new FileInputStream(configPath));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Could not load configuration file: " + configPath.getAbsolutePath() , e);
            }
        }

        RequestProcessingThreadPoolSize = Integer.parseInt(properties.getProperty("RequestProcessingThreadPoolSize"));
        ListenPort= Integer.parseInt(properties.getProperty("ListenPort"));
        SynthesizeTimeoutMs = Integer.parseInt(properties.getProperty("SynthesizeTimeoutMs"));
        UseSynthesizeEchoMode = Boolean.parseBoolean(properties.getProperty("UseSynthesizeEchoMode"));
        EchoModeDelayMs = Long.parseLong(properties.getProperty("EchoModeDelayMs"));
        SynthesisLogBucketName = properties.getProperty("SynthesisLogBucketName");
        EvidenceClasspath = properties.getProperty("EvidenceClasspath");
        AndroidJarPath = new File(properties.getProperty("AndroidJarPath"));
    }
}
