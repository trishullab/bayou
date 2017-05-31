package edu.rice.cs.caper.lib.service_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * A hook to be registered via Runtime.getRuntime().addShutdownHook(...).
 *
 * Calls .stopAsync() on the construction provided server when activated.
 */
public class ServiceServerShutdownHook extends Thread
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ServiceServerShutdownHook.class.getName());

    /**
     * The server to be stopped on activation.
     */
    private final ServiceServer _serviceServer;

    /**
     * @param serviceServer erver to be stopped on activation.
     */
    public ServiceServerShutdownHook(ServiceServer serviceServer)
    {
        _logger.debug("entering");
        _serviceServer = serviceServer;
    }

    @Override
    public void run()
    {
        _logger.debug("entering");

        /*
         * Shut the server down.
         */
        String className = _serviceServer.getClass().getName();
        _logger.info("Shutting down " + className + "...");
        try
        {
            _serviceServer.stopAsync();
        }
        catch (Throwable e)
        {
            for(Throwable i = e; i != null; i = i.getCause())
                _logger.error("Error stopping in shutdown hook.", i);
            _logger.debug("exiting");
            return; // don't want to block forever joining on a server that isn't stopping.
        }

        /*
         * Wait for the server to shut down.
         */
        try
        {
            _serviceServer.join(); // wait for server to actually stop
            _logger.info(className + " shut down.");
        }
        catch (InterruptedException e)
        {
            for(Throwable i = e; i != null; i = i.getCause())
                _logger.error("Error joining in shutdown hook.", i);
        }

        _logger.debug("exiting");
    }
}
