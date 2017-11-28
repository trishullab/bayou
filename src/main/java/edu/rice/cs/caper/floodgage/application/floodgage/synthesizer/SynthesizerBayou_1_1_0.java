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
package edu.rice.cs.caper.floodgage.application.floodgage.synthesizer;

import edu.rice.cs.caper.bayou.core.bayou_services_client.api_synthesis.ApiSynthesisClient;
import edu.rice.cs.caper.bayou.core.bayou_services_client.api_synthesis.SynthesisError;

import java.io.IOException;
import java.util.List;

/**
 * A Synthesizer that contacts a remote Bayou 1.1.0 server instance to perform synthesis.
 */
public class SynthesizerBayou_1_1_0 implements Synthesizer
{
    /**
     * Bayou server hostname.
     */
    private final String _host;

    /**
     * Bayou server port.
     */
    private final int _port;

    public SynthesizerBayou_1_1_0(String host, int port)
    {
        _host = host;
        _port = port;
    }

    @Override
    public List<String> synthesize(String draftProgram) throws SynthesizeException
    {
        try
        {
            return new ApiSynthesisClient(_host, _port).synthesise(draftProgram, 10);
        }
        catch (IOException | SynthesisError e)
        {
            throw new SynthesizeException(e);
        }
    }
}
