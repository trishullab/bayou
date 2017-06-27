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
