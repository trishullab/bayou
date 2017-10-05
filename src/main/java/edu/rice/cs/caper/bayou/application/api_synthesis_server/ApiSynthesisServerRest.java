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

import edu.rice.cs.caper.bayou.application.api_synthesis_server.servlet.ApiSynthesisHealthCheckServlet;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.servlet.ApiSynthesisResultQualityFeedbackServlet;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.servlet.ApiSynthesisServlet;
import edu.rice.cs.caper.programming.numbers.NatNum32;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;

/**
 * A REST server that accepts requests to synthesize holes in a given program when provided with API evidence of
 * the code completion for the hole.
 */
class ApiSynthesisServerRest
{
    /**
     * Indicates an error occurred when starting the server.
     */
    class StartErrorException extends Exception
    {
        StartErrorException(String message) { super(message); }

        StartErrorException(Throwable throwable)
        {
            super(throwable);
        }
    }

    /**
     * Place to send application logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisServerRest.class.getName());

    /**
     * The port on which to listen for incoming http connections.
     */
    private static final NatNum32 _httpListenPort = Configuration.ListenPort;

    /**
     * That maximum supported size of the body of a HTTP code completion request.
     */
    private static NatNum32 _codeCompletionRequestBodyMaxBytesCount =
            Configuration.CodeCompletionRequestBodyMaxBytesCount;

    /**
     * Track whether start has been called.
     */
    private boolean _startCalled = false;

    /**
     * Starts the server. Returns immediately. May only be called once.
     * @throws StartErrorException if there is a problem starting the server or this invocation of start is not the
     *                             first.
     */
    // we don't allow start to be called twice just because the HTTP listen port should still be in use from the
    // first start call since we have no stop action currently.
    void start() throws StartErrorException
    {
        _logger.debug("entering");

        if(_startCalled)
        {
            _logger.debug("exiting");
            throw new StartErrorException("Already started");
        }

        _startCalled = true;

        /*
         * Create and configure the HTTP server.
         */
        Server server = new Server(_httpListenPort.AsInt);
        {
            // Pattern as per https://www.eclipse.org/jetty/documentation/9.4.x/embedding-jetty.html
            ServletHandler handler = new ServletHandler();
            server.setHandler(handler);

            // register a servlet for performing apisynthesis
            handler.addServletWithMapping(ApiSynthesisServlet.class, "/apisynthesis");

            // register a servlet for collecting user feedback on result quatliy
            handler.addServletWithMapping(ApiSynthesisResultQualityFeedbackServlet.class, "/apisynthesisfeedback");

            // register a servlet for checking on the health of the entire apisynthesis process
            handler.addServletWithMapping(ApiSynthesisHealthCheckServlet.class, "/apisynthesishealth");

            /*
             * Code completion requests are sent via POST to ApiSynthesisServlet, however,
             * the site URL for the page that sends those POST requests can house that request body as a query parameter
             * for bookmarking.  That bookmarked URL then becomes the referrer of the POST request. As such there is a
             * relationship between the required header buffer size for this server and the allowed body size.
             *
             * As such ensure that we can accept headers as large as our max body size.
             */
            for (Connector c : server.getConnectors())
            {
                HttpConfiguration config = c.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
                config.setRequestHeaderSize(_codeCompletionRequestBodyMaxBytesCount.AsInt);
            }
        }

        /*
         * Start the HTTP server.
         */
        try
        {
            server.start(); // returns immediately
            _logger.info("Started HTTP server on port " + _httpListenPort);
        }
        catch (Throwable e)
        {
            throw new StartErrorException(e);
        }

        _logger.debug("exiting");
    }
}
