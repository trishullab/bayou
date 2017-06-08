package edu.rice.cs.caper.programs.bayou.api_synthesis_server;

import edu.rice.cs.caper.lib.service_server.ServiceServer;
import edu.rice.cs.caper.lib.service_server.ServiceServerLauncher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ApiSynthesisServerMain
{
    /**
     * Place to send program logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisServerMain.class.getName());

    /*
     * Register a global exception logger.
     */
    static
    {
        Thread.setDefaultUncaughtExceptionHandler((Thread t, Throwable e) ->
        {
            if(_logger == null)
            {
                e.printStackTrace();
                return;
            }

            for(Throwable i = e; i != null; i = i.getCause())
                _logger.error(t, i);

        });
    }

    public static void main(String[] args)
    {
        _logger.debug("entering");

        /*
         * Construct a new api synthesis server.
         */
        try
        {
            new ApiSynthesisServerRest().start();
        }
        catch (Throwable e)
        {
            _logger.fatal("Error creating ApiSynthesisServer", e);
            _logger.debug("exiting");
            System.exit(1);
        }

        _logger.debug("exiting");
    }
}
