/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;

import edu.rice.cs.caper.programming.numbers.NatNum32;
import org.junit.Assert;
import org.junit.Test;

public class ApiSynthesizerEchoTests
{
    private static final NatNum32 ONE = new NatNum32(1);

    @Test(expected = IllegalArgumentException.class)
    public void testConstructionNegativeDelay() throws SynthesiseException
    {
        new ApiSynthesizerEcho(-1);
    }


    @Test
    public void testSynthesise1() throws SynthesiseException
    {
        String result = new ApiSynthesizerEcho(50).synthesise("code", ONE).iterator().next();

        Assert.assertEquals("code", result);
    }

    @Test
    public void testSynthesise2() throws SynthesiseException
    {
        String result = new ApiSynthesizerEcho(50).synthesise("code", ONE).iterator().next();

        Assert.assertEquals("code", result);
    }
}
