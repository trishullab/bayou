package edu.rice.cs.caper.lib.service_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A template for:
 *
 *    1.) Creating a sever.
 *    2.) Registering a shutdown hook to stop the server on shutdown.
 *    3.) Starting the created server.
 */
public interface ServiceServerLauncher
{
    /**
     * 1.) Creates a server via makeServer().
     * 2.) Registering a shutdown hook to stop the server on shutdown via  Runtime.getRuntime().addShutdownHook(...)
     * 3.) Invokes startAsync() on the created server.
     */
    default void launchAsync()
    {
        /*
         * Place to send logging information.
         */
        Logger logger = LogManager.getLogger(ServiceServerLauncher.class.getName());
        logger.debug("entering ");

        ServiceServer server = makeServer();

        /*
         * Add a shutdown hook so we can intercept Ctrl+c and close any connected client sockets
         * and log that the server is stopping.
         */
        Runtime.getRuntime().addShutdownHook(new ServiceServerShutdownHook(server));

        /*
         * Start the server.
         */
        logger.info("Starting " + server.getClass().getName());
        server.startAsync();
        logger.debug("exiting ");
    }

    /**
     * Creates instances of the server to be launched.
     * @return the server to start.
     */
    ServiceServer makeServer();
}
