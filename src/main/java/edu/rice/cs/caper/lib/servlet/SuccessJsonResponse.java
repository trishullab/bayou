package edu.rice.cs.caper.lib.servlet;

import edu.rice.cs.caper.lib.servlet.ErrorJsonResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

/**
 * Base class for JSON objects that represent success in HTTP responses.
 */
public class SuccessJsonResponse extends JSONObject
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ErrorJsonResponse.class.getName());

    public SuccessJsonResponse()
    {
        _logger.debug("entering");
        put("success", true);
        _logger.debug("exiting");
    }
}
