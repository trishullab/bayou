package edu.rice.cs.caper.bayou.program.api_synthesis_server;

import org.json.JSONArray;
import org.json.JSONObject;

class SynthesisResponseSuccess extends JSONObject
{
    SynthesisResponseSuccess(Iterable<String> results)
    {
        if(results == null)
            throw new IllegalArgumentException("results may not be null");

        put("success", true);

        JSONArray resultsArray = new JSONArray();

        for(String result : results)
        {
            if(result != null)
            {
                resultsArray.put(result);
            }
        }

        put("results", resultsArray);
    }
}
