package edu.rice.cs.caper.bayou.program.api_synthesis_server.synthesis_logging;

import java.util.List;
import java.util.UUID;

/**
 * Records the input and solutions to a code synthesis request.
 */
public interface SynthesisLogger
{
    /**
     * Logs a synthesis input and solutions for a given request.
     *
     * @param requestId a unique identifier for the request.
     * @param searchCode the input code of the request.
     * @param results the generated synthesis solutions for the request.
     */
    void log(UUID requestId, String searchCode, List<String> results);
}
