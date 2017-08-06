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
package edu.rice.cs.caper.bayou.application.api_synthesis_server;

import edu.rice.cs.caper.bayou.core.synthesizer.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

/**
 * Synthesizes by returning the given searchCode after some construction figured simulated latency.
 */
public class ApiSynthesisStrategyEcho implements ApiSynthesisStrategy
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesisStrategyEcho.class.getName());

    /**
     * How long to sleep in synthesise before returning a result.  Must be >=0.
     */
    private final long _delayMs;

    /**
     * @param delayMs How long to sleep in each invocation of synthesise(...) before returning.  Must be >=0.
     * @throws IllegalArgumentException if delayMs < 0.
     */
    public ApiSynthesisStrategyEcho(long delayMs)
    {
        _logger.debug("entering");

        if(delayMs < 0)
            throw new IllegalArgumentException("delayMs must be >= 0. Found: " + delayMs);

        _delayMs = delayMs;

        _logger.debug("exiting");
    }

    @Override
    public Iterable<String> synthesise(String searchCode) throws SynthesiseException
    {
        _logger.debug("entering");

        if(_delayMs > 0)
        {
            try
            {
                Thread.sleep(_delayMs);
            }
            catch (InterruptedException e)
            {
                _logger.debug("exiting");
                throw new SynthesiseException(e);
            }
        }

        _logger.debug("exiting");
        return Collections.singletonList(searchCode);
    }

    @Override
    public Iterable<String> synthesise(String searchCode, int sampleCount) throws SynthesiseException, ParseException
    {
        return synthesise(searchCode);
    }
}
