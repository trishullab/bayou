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
package edu.rice.cs.caper.bayou.application.api_synthesis_server.servlet;

import edu.rice.cs.caper.bayou.application.api_synthesis_server.Configuration;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis.ApiSynthesizer;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis.ApiSynthesizerEcho;
import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis.ApiSynthesizerRemoteTensorFlowAsts;
import edu.rice.cs.caper.programming.ContentString;
import edu.rice.cs.caper.programming.numbers.NatNum32;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.text.AbstractDocument;

/**
 * A factory capable of constructing an ApiSynthesizer as specified by the system configuration.
 */
class ApiSynthesizerFactory
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(ApiSynthesizerFactory.class.getName());

    /**
     * @return a synthesiser in accordence with edu.rice.cs.caper.bayou.application.api_synthesis_server.Configuration
     */
    static ApiSynthesizer makeFromConfig()
    {
        _logger.debug("entering");

        ApiSynthesizer synthesizer;
        if(Configuration.UseSynthesizeEchoMode)
        {
            synthesizer = new ApiSynthesizerEcho(Configuration.EchoModeDelayMs);
        }
        else
        {

            ContentString tensorFlowHost = new ContentString(Configuration.AstServerAuthority.split(":")[0]);
            NatNum32 tensorFlowPort = new NatNum32(Configuration.AstServerAuthority.split(":")[1]);
            synthesizer = new ApiSynthesizerRemoteTensorFlowAsts(tensorFlowHost, tensorFlowPort,
                                                                 Configuration.SynthesizeTimeoutMs,
                                                                 Configuration.EvidenceClasspath,
                                                                 Configuration.AndroidJarPath, Configuration.ApiSynthMode);
        }

        _logger.debug("exiting");
        return synthesizer;
    }
}
