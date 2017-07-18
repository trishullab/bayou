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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Allows a set of "allowed origins" to be defined and applies any such origin to an http response
 * Access-Control-Allow-Origin header if found as the origin in a given request.
 */
public class CorsHeaderSetter
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(CorsHeaderSetter.class.getName());

    /**
     * The set of origins that may be added to http responses.
     */
    private final Set<String> _allowedOrigins;

    /**
     * @param allowedOrigins The set of origins that may be added to http responses.
     */
    public CorsHeaderSetter(String... allowedOrigins)
    {
        this(new HashSet<>(Arrays.asList(allowedOrigins)));
    }

    /**
     * @param allowedOrigins The set of origins that may be added to http responses.
     */
    CorsHeaderSetter(Set<String> allowedOrigins)
    {
        _logger.debug("entering");
        _logger.trace("allowedOrigins:" + allowedOrigins);
        _allowedOrigins = allowedOrigins;
        _logger.debug("exiting");
    }

    /**
     * If the given request's origin is present and a member of the allowed origins,
     * sets the response Access-Control-Allow-Origin header to the request's origin.
     *
     * @param req the http request
     * @param resp the http response
     */
    public boolean applyAccessControlHeaderIfAllowed(HttpServletRequest req, HttpServletResponse resp)
    {
        _logger.debug("entering");

        String origin = req.getHeader("Origin");

        if(origin == null)
        {
            _logger.debug("exiting");
            return false;
        }


        if(_allowedOrigins.contains(origin))
        {
            resp.setHeader("Access-Control-Allow-Origin", origin);
            _logger.debug("exiting");
            return true;
        }

        _logger.debug("exiting");
        return false;
    }

}
