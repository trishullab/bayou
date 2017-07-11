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
import static org.mockito.Mockito.*;

import edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis_logging.SynthesisQualityFeedbackLogger;
import org.json.JSONObject;
import org.junit.Test;

import java.util.UUID;

public class ApiSynthesisResultQualityFeedbackServletTest
{
    @Test(expected = NullPointerException.class)
    public void testDecodeBodyAndLogNullLogger()
    {
        ApiSynthesisResultQualityFeedbackServlet.decodeBodyAndLog(null, null);
    }

    @Test
    public void testDecodeBodyAndLog()
    {
        String requestId = "2d371e10-354c-4df5-bead-41321b5750d6";
        String searchCode = "code1";
        String resultCode = "code2";

        JSONObject body = new JSONObject();
        body.put("requestId", requestId);
        body.put("searchCode", searchCode);
        body.put("resultCode", resultCode);
        body.put("isGood", true);

        SynthesisQualityFeedbackLogger logger = mock(SynthesisQualityFeedbackLogger.class);
        ApiSynthesisResultQualityFeedbackServlet.decodeBodyAndLog(body, logger);

        verify(logger).log(UUID.fromString(requestId), searchCode, resultCode, true);
    }
}
