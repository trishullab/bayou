package edu.rice.cs.caper.bayou.application.api_synthesis_server;

import edu.rice.cs.caper.servlet.CorsHeaderSetter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

interface CorsAwareServlet
{
    /**
     * If the given request's origin is present and a member of the allowed origins as decided by corsHeaderSetter
     * sets the response Access-Control-Allow-Origin header to the request's origin.
     *
     * If not, sets the response code to 403 via resp.
     *
     * @param req the http request
     * @param resp the http response
     * @param corsHeaderSetter the source of truth on allowed origins
     * @return whether req represents a request from an allowed origin.
     */
    default boolean applyAccessControlHeaderIfAllowed(HttpServletRequest req, HttpServletResponse resp,
                                                      CorsHeaderSetter corsHeaderSetter)
    {
        /*
         * Place to send logging information.
         */
        Logger _logger = LogManager.getLogger(CorsAwareServlet.class.getName());
        _logger.debug("entering");

        // only allow CORS processing from origins we approve
        boolean requestFromAllowedOrigin = corsHeaderSetter.applyAccessControlHeaderIfAllowed(req, resp);

        if(!requestFromAllowedOrigin)
        {
            resp.setStatus(HttpStatus.FORBIDDEN_403);
            _logger.warn("request from unauthorized origin.");
        }

        _logger.debug("exiting");
        return requestFromAllowedOrigin;
    }
}
