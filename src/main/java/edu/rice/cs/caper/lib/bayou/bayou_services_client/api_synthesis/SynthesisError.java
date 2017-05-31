package edu.rice.cs.caper.lib.bayou.bayou_services_client.api_synthesis;

/**
 * Created by barnett on 3/15/17.
 */
public class SynthesisError extends Exception
{
    public SynthesisError(String msg)
    {
        super(msg);
    }

    public SynthesisError(Throwable cause)
    {
        super(cause);
    }
}
