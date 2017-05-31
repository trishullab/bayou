package edu.rice.cs.caper.programs.bayou.api_synthesis_server;

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
}
