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
        ApiSynthesisServer server;
        try
        {
            ExecutorService threadPool = Executors.newFixedThreadPool(Configuration.RequestProcessingThreadPoolSize);

            ApiSynthesisStrategy synthesisStrategy;
            if(Configuration.UseSynthesizeEchoMode)
            {
                synthesisStrategy = new ApiSynthesisStrategyEcho(Configuration.EchoModeDelayMs);
            }
            else
            {
                synthesisStrategy =
                        new ApiSynthesisStrategyRemoteTensorFlowAsts("localhost", 8084,
                                Configuration.SynthesizeTimeoutMs,
                                Configuration.EvidenceClasspath, Configuration.AndroidJarPath);
            }

            server = new ApiSynthesisServer(Configuration.ListenPort, threadPool, synthesisStrategy);
        }
        catch (Throwable e)
        {
            _logger.fatal("Error creating ApiSynthesisServer", e);
            System.exit(1);
            return; // for static analysis to remove uninitialized error on "return server;" below in IntelliJ
        }

        /*
         * Start the sever.
         */
        //noinspection Convert2Lambda -- obfuscates instantiation of ServiceServerLauncher
        new ServiceServerLauncher()
        {
            @Override
            public ServiceServer makeServer()
            {
                return server;
            }
        }.launchAsync();

        _logger.debug("exiting");
    }
}
