package edu.rice.cs.caper.bayou.application.api_synthesis_server;

/**
 * A method for synthesizing code from given code.
 */
interface ApiSynthesisStrategy
{
    class SynthesiseException extends Exception
    {
        SynthesiseException(Throwable cause)
        {
            super(cause);
        }

        SynthesiseException(String msg)
        {
            super(msg);
        }
    }

    Iterable<String> synthesise(String searchCode) throws SynthesiseException;

    static ApiSynthesisStrategy fromConfig()
    {
        if(Configuration.UseSynthesizeEchoMode)
        {
            return  new ApiSynthesisStrategyEcho(Configuration.EchoModeDelayMs);
        }
        else
        {
           return new ApiSynthesisStrategyRemoteTensorFlowAsts("localhost", 8084,
                                                               Configuration.SynthesizeTimeoutMs,
                                                               Configuration.EvidenceClasspath,
                                                               Configuration.AndroidJarPath);
        }

    }
}
