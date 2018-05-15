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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * A SynthesisLogger that stores the log message in a local file. Stores log messages as a JSON string using UTF-8 encoding.
 */
public class SynthesisLoggerLocal implements SynthesisLogger {

    /**
     * Place to send application logging information.
     */
    private static final Logger _logger = LogManager.getLogger(SynthesisLoggerLocal.class.getName());

    @Override
    public void log(UUID requestId, String searchCode, Iterable<String> results)
    {
        _logger.debug("entering");

        /*
         * Make the log message to store in local file.
         */
        String logMsg;
        {
            long now = System.currentTimeMillis();

            JSONObject recordObj = new JSONObject();
            recordObj.put("version", "1");
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

        String objectKey = requestId.toString() + ".txt";
        try {
            Files.write(new File("logs", objectKey).toPath(), logMsg.getBytes());
        } catch (IOException e) {
            _logger.debug("could not log synth request");
        }

        _logger.debug("exiting");
    }

}
