package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging;

import java.util.UUID;

/**
 * Records whether the given result for the given search code is considered a good result.
 */
public interface SynthesisQualityFeedbackLogger
{
    /**
     * Records whether the given result for the given search code is considered a good result.
     *
     * @param requestId the request that generated resultCode
     * @param searchCode the original request code
     * @param resultCode one of the results of teh request
     * @param isGood whether the result should be considered good or bad.
     */
    void log(UUID requestId, String searchCode, String resultCode, boolean isGood);
}
