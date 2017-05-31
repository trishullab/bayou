package edu.rice.cs.caper.programs.bayou.api_synthesis_server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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

    static
    {
        Properties properties = new Properties();
        {
            String propertyKey = "configurationFile";

            String configPath = System.getProperty(propertyKey) != null ? System.getProperty(propertyKey) :
                                                                          "apiSynthesisServerConfig.properties";

            try
            {
                properties.load(new FileInputStream(configPath));
            }
            catch (IOException e)
            {
                throw new RuntimeException("Could not load configuration file.", e);
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
