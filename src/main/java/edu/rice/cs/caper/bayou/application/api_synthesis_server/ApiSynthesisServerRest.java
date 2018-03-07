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
import edu.rice.cs.caper.servlet.ServerIdHttpServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

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
     * The port on which to listen for incoming http connections to service non-heartbeat requests.
     */
    private static final NatNum32 _httpRequestListenPort = Configuration.RequestListenPort;

    /**
     * The port on which to listen for incoming http connections to service heartbeat requests.
     */
    private static final NatNum32 _httpHeartbeatListenPort = Configuration.HeartbeatListenPort;

    /**
     * That maximum supported size of the body of a HTTP code completion request.
     */
    private static NatNum32 _codeCompletionRequestBodyMaxBytesCount =
            Configuration.CodeCompletionRequestBodyMaxBytesCount;

    /*
     * The number of tasks allowed in the Jetty task queue before new requests are rejected.
     */
    private static NatNum32 _jettyTaskQueueSize = Configuration.JettyTaskQueueSize;

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
         * Create and configure the HTTP server that processes non-heartbeat related requests.
         */
        Server apiSynthServer;
        {
            /*
             * Set a bounded request queue because the default Jetty queue is unbounded and we want to defend
             * against an attack that makes a large volume of requests and exhausts the process' memory.
             */
            LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(_jettyTaskQueueSize.AsInt);
            ExecutorThreadPool pool = new ExecutorThreadPool(10, 20, 60, TimeUnit.SECONDS, queue);
            apiSynthServer = new Server(pool);

            /*
             * Configure the server to listen for requests on port _httpRequestListenPort.
             */
            ServerConnector connector = new ServerConnector(apiSynthServer, new HttpConnectionFactory());
            connector.setPort(_httpRequestListenPort.AsInt);
            apiSynthServer.addConnector(connector);

            /*
             * Configure routes.
             *
             * Pattern as per https://www.eclipse.org/jetty/documentation/9.4.x/embedding-jetty.html
             */
            ServletHandler handler = new ServletHandler();
            apiSynthServer.setHandler(handler);

            // register a servlet for performing apisynthesis
            handler.addServletWithMapping(ApiSynthesisServlet.class, "/apisynthesis");

            // register a servlet for collecting user feedback on result quality
            handler.addServletWithMapping(ApiSynthesisResultQualityFeedbackServlet.class, "/apisynthesisfeedback");

            /*
             * Code completion requests are sent via POST to ApiSynthesisServlet, however,
             * the site URL for the page that sends those POST requests can house that request body as a query parameter
             * for bookmarking.  That bookmarked URL then becomes the referrer of the POST request. As such there is a
             * relationship between the required header buffer size for this server and the allowed body size.
             *
             * As such ensure that we can accept headers as large as our max body size.
             */
            for (Connector c : apiSynthServer.getConnectors())
            {
                HttpConfiguration config = c.getConnectionFactory(HttpConnectionFactory.class).getHttpConfiguration();
                config.setRequestHeaderSize(_codeCompletionRequestBodyMaxBytesCount.AsInt);
            }
        }

        /*
         * Create and configure the HTTP server that processes heart beat requests.
         *
         * Use a different server than apiSynthServer because we don't want heart beat checks to be rejected
         * due to the bounded task queue used for apiSynthServer and inadvertently signal the system is unhealthy.
         *
         * We assume that the system is deployed in a way such that attackers do not have access to the heart beat
         * port and as such all heart beat requests are genuine (e.g. rate limited) and non-abusive.
         */
        Server healthCheckServer = new Server(_httpHeartbeatListenPort.AsInt);
        {
            /*
             * Configure routes.
             */
            // Pattern as per https://www.eclipse.org/jetty/documentation/9.4.x/embedding-jetty.html
            ServletHandler handler = new ServletHandler();
            healthCheckServer.setHandler(handler);

            // register a servlet for checking on the health of the entire apisynthesis process
            // only allow requests on port _httpHeartbeatListenPort
            handler.addServletWithMapping(ApiSynthesisHealthCheckServlet.class, "/apisynthesishealth");
        }

        /*
         * Set the server id for response headers.
         */
        String serverId = UUID.randomUUID().toString();
        ServerIdHttpServlet.setServerId(serverId);
        _logger.info("Server id is: " + serverId);

        /*
         * Start the HTTP servers.
         */
        try
        {
            healthCheckServer.start(); // returns immediately
            _logger.info("Started HTTP heartbeat server on port " + _httpHeartbeatListenPort);
            apiSynthServer.start(); // returns immediately
            _logger.info("Started HTTP synth server on port " + _httpRequestListenPort);
        }
        catch (Throwable e)
        {
            _logger.debug("exiting");
            throw new StartErrorException(e);
        }

        _logger.debug("exiting");
    }
}
