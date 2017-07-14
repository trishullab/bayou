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
