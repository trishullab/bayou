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

import com.amazonaws.services.dynamodbv2.xspec.S;
import edu.rice.cs.caper.programming.numbers.NatNum32;
import edu.rice.cs.caper.servlet.ErrorJsonResponse;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;

interface SynthesisRequest
{
    class NoCodeFieldException extends Exception { }
    class NoMaxProgramCountFieldException extends Exception { }
    class InvalidMaxProgramCountException extends Exception { }
    class InvalidSampleCountException extends  Exception { }

    String getCode();

    NatNum32 getMaxProgramCount();

    NatNum32 getOptionalSampleCount(NatNum32 onNotPresent);

    static SynthesisRequest make(JSONObject jsonMessage) throws NoCodeFieldException, NoMaxProgramCountFieldException,
            InvalidMaxProgramCountException, InvalidSampleCountException
    {
        final String CODE = "code";
        if(!jsonMessage.has(CODE))
            throw new NoCodeFieldException();

        String code = jsonMessage.get(CODE).toString();


        NatNum32 maxProgramCount;
        try
        {
            final String MAX_PROGRAM_COUNT = "max program count";

            if (!jsonMessage.has(MAX_PROGRAM_COUNT))
                throw new NoMaxProgramCountFieldException();

            maxProgramCount = new NatNum32(jsonMessage.get(MAX_PROGRAM_COUNT).toString());
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidMaxProgramCountException();
        }

        NatNum32 sampleCount;
        try
        {
            final String SAMPLE_COUNT = "sample count";

            if (!jsonMessage.has(SAMPLE_COUNT))
                sampleCount = null;
            else
                sampleCount = new NatNum32(jsonMessage.get(SAMPLE_COUNT).toString());
        }
        catch (IllegalArgumentException e)
        {
            throw new InvalidSampleCountException();
        }


        return new SynthesisRequest()
        {
            @Override
            public String getCode()
            {
                return code;
            }

            @Override
            public NatNum32 getMaxProgramCount()
            {
                return maxProgramCount;
            }

            @Override
            public NatNum32 getOptionalSampleCount(NatNum32 onNotPresent)
            {
                return sampleCount != null ? sampleCount : onNotPresent;
            }
        };
    }


}
