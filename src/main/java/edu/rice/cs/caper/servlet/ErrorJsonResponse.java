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
package edu.rice.cs.caper.servlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * A JSON object to be used as HTTP response bodies in the case of an error.
 */
public class ErrorJsonResponse extends JSONObject
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ErrorJsonResponse.class.getName());

    public ErrorJsonResponse(String errorMessage)
    {
        _logger.debug("entering");
        put("success", false);
        put("errorMessage", errorMessage);
        _logger.debug("exiting");
    }
}
