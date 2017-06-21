package edu.rice.cs.caper.bayou.program.api_synthesis_server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

/**
 * Synthesizes by returning the given searchCode after some construction figured simulated latency.
 */
public class ApiSynthesisStrategyEcho implements ApiSynthesisStrategy
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisStrategyEcho.class.getName());

    /**
     * How long to sleep in synthesise before returning a result.  Must be >=0.
     */
    private final long _delayMs;

    /**
     * @param delayMs How long to sleep in each invocation of synthesise(...) before returning.  Must be >=0.
     * @throws IllegalArgumentException if delayMs < 0.
     */
    public ApiSynthesisStrategyEcho(long delayMs)
    {
        _logger.debug("entering");

        if(delayMs < 0)
            throw new IllegalArgumentException("delayMs must be >= 0. Found: " + delayMs);

        _delayMs = delayMs;

        _logger.debug("exiting");
    }

    @Override
    public Iterable<String> synthesise(String searchCode) throws SynthesiseException
    {
        _logger.debug("entering");

        if(_delayMs > 0)
        {
            try
            {
                Thread.sleep(_delayMs);
            }
            catch (InterruptedException e)
            {
                _logger.debug("exiting");
                throw new SynthesiseException(e);
            }
        }

        _logger.debug("exiting");
        return Collections.singletonList(searchCode);
    }
}
