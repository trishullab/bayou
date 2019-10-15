package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class SynthesisLoggerNone implements SynthesisLogger
{
    /**
     * Place to send application logging information.
     */
    private final Logger _logger = LogManager.getLogger(this.getClass());

    @Override
    public void log(UUID requestId, String searchCode, Iterable<String> results)
    {
        _logger.debug("entering");
        // do nothing
        _logger.debug("exiting");
    }
}
