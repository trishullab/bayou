package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;

public class SynthesiseException extends Exception
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
