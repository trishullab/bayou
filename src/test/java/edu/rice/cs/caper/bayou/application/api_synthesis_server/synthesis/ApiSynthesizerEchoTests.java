package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;

import org.junit.Assert;
import org.junit.Test;

public class ApiSynthesizerEchoTests
{

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionNegativeDelay() throws SynthesiseException
    {
        new ApiSynthesizerEcho(-1);
    }


    @Test
    public void testSynthesise1() throws SynthesiseException
    {
        String result = new ApiSynthesizerEcho(50).synthesise("code", 1).iterator().next();

        Assert.assertEquals("code", result);
    }

    @Test
    public void testSynthesise2() throws SynthesiseException
    {
        String result = new ApiSynthesizerEcho(50).synthesise("code", 1, 3).iterator().next();

        Assert.assertEquals("code", result);
    }
}
