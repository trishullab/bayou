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

    Iterable<String> synthesise(String searchCode) throws SynthesiseException, ParseException;

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
