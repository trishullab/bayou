package edu.rice.cs.caper.bayou.program.api_synthesis_server;

import org.json.JSONObject;

class SynthesisResponseError extends JSONObject
{
    SynthesisResponseError(String errorMessage)
    {
        if(errorMessage == null)
            throw new IllegalArgumentException("errorMessage may not be null");

        put("success", false);
        put("errorMessage", errorMessage);
    }
}
