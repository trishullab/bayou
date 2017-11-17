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

import edu.rice.cs.caper.bayou.core.synthesizer.Synthesizer;
import edu.rice.cs.caper.programming.ContentString;
import edu.rice.cs.caper.programming.numbers.NatNum32;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Configuration options for the Api Synthesis Server application.
 */
public class Configuration
{
    static final NatNum32 ListenPort;

    public static final NatNum32 SynthesizeTimeoutMs;

    public static final boolean UseSynthesizeEchoMode;

    public static final long EchoModeDelayMs;

    public static final String SynthesisLogBucketName;

    public static final String SynthesisQualityFeedbackLogBucketName;

    public static final ContentString EvidenceClasspath;

    public static final File AndroidJarPath;

    private static final int MEGA_BYTES_IN_BYTES = 1000000;

    public static NatNum32 CodeCompletionRequestBodyMaxBytesCount = new NatNum32(MEGA_BYTES_IN_BYTES);

    public static final String[] CorsAllowedOrigins;

    public static final Synthesizer.Mode ApiSynthMode;

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

        ListenPort= NatNum32.parse(properties.getProperty("ListenPort"));
        SynthesizeTimeoutMs = NatNum32.parse(properties.getProperty("SynthesizeTimeoutMs"));
        UseSynthesizeEchoMode = Boolean.parseBoolean(properties.getProperty("UseSynthesizeEchoMode"));
        EchoModeDelayMs = Long.parseLong(properties.getProperty("EchoModeDelayMs"));
        SynthesisLogBucketName = properties.getProperty("SynthesisLogBucketName");
        SynthesisQualityFeedbackLogBucketName = properties.getProperty("SynthesisQualityFeedbackLogBucketName");
        EvidenceClasspath = new ContentString(properties.getProperty("EvidenceClasspath"));
        AndroidJarPath = new File(properties.getProperty("AndroidJarPath"));
        CorsAllowedOrigins = properties.getProperty("CorsAllowedOrigins").split("\\s+"); // split by whitespace
        ApiSynthMode = Synthesizer.Mode.valueOf(properties.getProperty("ApiSynthMode"));


    }
}
