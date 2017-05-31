package edu.rice.cs.caper.programs.bayou.api_synthesis_server;

import edu.rice.cs.caper.lib.service_server.ServiceServer;
import edu.rice.cs.caper.lib.service_server.ServiceServerExecutorDispatch;
import edu.rice.cs.caper.lib.service_server.ServiceServerLauncher;
import edu.rice.cs.caper.programs.bayou.api_synthesis_server.synthesis_logging.SynthesisLogger;
import edu.rice.cs.caper.programs.bayou.api_synthesis_server.synthesis_logging.SynthesisLoggerS3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ApiSynthesisServer extends ServiceServerExecutorDispatch
{
    /**
     * Place to send program logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisServer.class.getName());


    /**
     * A triple of RequestId, SearchCode, Results.
     */
    private class SynthesisLogMessage
    {
        final UUID RequestId;

        final String SearchCode;

        final List<String> Results;

        SynthesisLogMessage(UUID requestId, String searchCode, List<String> results)
        {
            RequestId = requestId;
            SearchCode = searchCode;
            Results = results;
        }
    }

    /**
     * Place to send synthesis request completion information.
     */
    private final SynthesisLogger _synthesisLogger = new SynthesisLoggerS3(Configuration.SynthesisLogBucketName);

    /**
     * A buffer shared among threads that produce synth results and a logging thread that consumes log messages
     * and sends them to a logging destination.
     *
     * ALL USE OF _synthResultsToLog MUST BE PROTECTED TAKING THE OBJECT'S LOCK.
     */
    private final List<SynthesisLogMessage> _synthResultsToLog = new LinkedList<>();

    /**
     * How requests should be fulfilled.
     */
    private final ApiSynthesisStrategy _synthStrategy;

    ApiSynthesisServer(int listenPort, ExecutorService requestProcessingThreadPool,
                       ApiSynthesisStrategy synthesisStrategy) throws IOException
    {
        super(listenPort, requestProcessingThreadPool);
        _synthStrategy = synthesisStrategy;

        /*
         * Launch a thread to periodically poll _synthResultsToLog for messages and if so log them to _synthesisLogger.
         */
        new Thread(() ->
        {
            boolean canSynthResultsToLogEverBeNonEmpty = true;
            while(canSynthResultsToLogEverBeNonEmpty)
            {
                try
                {
                    // n.b. we use a "copy to private buffer and log from buffer" strategy here so
                    // that we we don't hold the _synthResultsToLog lock while calling log(). Logging could be a long
                    // operation involving the network so let's not tie up the lock.

                    /*
                     * Wait for _synthResultsToLog to become non-empty, copy all contents to toLog, and clear
                     * _synthResultsToLog.
                     */
                    List<SynthesisLogMessage> toLog = new LinkedList<>();
                    synchronized (_synthResultsToLog)
                    {
                        if (_synthResultsToLog.isEmpty())
                            _synthResultsToLog.wait(); // notify signals non-empty

                        toLog.addAll(_synthResultsToLog);
                        _synthResultsToLog.clear();

                        if(isAllClientProcessingComplete()) // no more client request activity possible and
                            canSynthResultsToLogEverBeNonEmpty = false; // _synthResultsToLog is empty
                    }

                    /*
                     * Log all results.
                     */
                    for(SynthesisLogMessage msg : toLog)
                        _synthesisLogger.log(msg.RequestId, msg.SearchCode, msg.Results);
                }
                catch (Throwable e)
                {
                    for(Throwable i = e; i != null; i = i.getCause())
                        _logger.error(i.getMessage(), i);
                }
            }
        }).start();
    }

    @Override
    protected JSONObject processClientRequest(UUID requestId, JSONObject requestMessage)
    {
        _logger.debug("entering");

        /*
         * Determine the search code provided by the request.
         */
        String searchCode;
        {
            final String CODE = "code";

            if (!requestMessage.has(CODE))
            {
                _logger.debug("exiting");
                return new SynthesisResponseError("Expected code field in request message");
            }

            try
            {
                searchCode = requestMessage.getString(CODE);
            }
            catch (JSONException e)
            {
                _logger.debug("exiting");
                return new SynthesisResponseError("Expected code field in request message to be a string");
            }
        }

        /*
         * Synthesise from the search code and store results in results.
         */
        List<String> results = new LinkedList<>();
        try
        {
            for(String result : _synthStrategy.synthesise(searchCode))
            {
                if (result == null)
                    continue;

                if (result.trim().length() == 0)
                    continue;

                results.add(result);
            }

        }
        catch (ApiSynthesisStrategy.SynthesiseException e)
        {
            for(Throwable i = e; i != null; i = i.getCause())
	            _logger.error(i);

            String msg = e.getMessage();

            if(msg == null)
                msg = "Exception in synthesise(...)";

            _logger.debug("exiting");
            return new SynthesisResponseError(msg);
        }

        /*
         * Enqueue results for logging.
         */
        synchronized (_synthResultsToLog)
        {
            // results is returned so make private copy for SynthesisLogEvent in case modified
            SynthesisLogMessage msg = new SynthesisLogMessage(requestId, searchCode, new LinkedList<>(results));

            _synthResultsToLog.add(msg);
            _synthResultsToLog.notify(); // signal non-empty.  May already be non-empty before add. That's ok.
        }

        _logger.debug("exiting");
        return new SynthesisResponseSuccess(results);
    }


    @Override
    protected JSONObject makeUnexpectedErrorResponse(UUID requestId)
    {
        return new SynthesisResponseError("Unexpected error.");
    }

    @Override
    protected JSONObject makeEarlyEndOfStreamDetectedErrorResponse()
    {
        return new SynthesisResponseError("Early end of stream detected.");
    }


}
