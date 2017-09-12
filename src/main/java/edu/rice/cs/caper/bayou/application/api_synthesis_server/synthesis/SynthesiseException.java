package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;

/**
 * Thrown to indicate a problem arose during synthesis. 
 */
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
