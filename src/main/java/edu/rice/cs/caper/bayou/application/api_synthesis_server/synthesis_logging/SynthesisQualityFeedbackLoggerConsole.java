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
package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging;

import java.util.UUID;

/**
 * A SynthesisQualityFeedbackLogger that writes all args to System.out.
 */
public class SynthesisQualityFeedbackLoggerConsole implements SynthesisQualityFeedbackLogger
{
    @Override
    public void log(UUID requestId, String searchCode, String resultCode, boolean isGood)
    {
        System.out.println("requestId: " + requestId);
        System.out.println("searchCode: " + searchCode);
        System.out.println("resultCode: " + resultCode);
        System.out.println("isGood: " + isGood);
    }
}
