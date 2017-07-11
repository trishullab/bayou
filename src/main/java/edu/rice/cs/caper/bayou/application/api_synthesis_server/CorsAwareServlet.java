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
