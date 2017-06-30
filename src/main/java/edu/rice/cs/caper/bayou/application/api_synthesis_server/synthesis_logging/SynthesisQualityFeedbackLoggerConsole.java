package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging;

import java.util.UUID;

/**
 * A SynthesisQualityFeedbackLogger that writes all args to System.out.
 */
public class SynthesisQualityFeedbackLoggerConsole implements SynthesisQualityFeedbackLogger
{
    @Override
    public void log(UUID requestId, String searchCode, String resultCode, boolean isGood)
    {
        System.out.println("requestId: " + requestId);
        System.out.println("searchCode: " + searchCode);
        System.out.println("resultCode: " + resultCode);
        System.out.println("isGood: " + isGood);
    }
}
