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


import edu.rice.cs.caper.bayou.core.bayou_services_client.api_synthesis.ApiSynthesisClient;
import edu.rice.cs.caper.bayou.core.bayou_services_client.api_synthesis.ParseError;
import edu.rice.cs.caper.bayou.core.bayou_services_client.api_synthesis.SynthesisError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

class ApiSynthesisLocalClient
{
    private static final String _testDialog =
            "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                    "import android.content.Context;\n" +
                    "\n" +
                    "public class TestDialog {\n" +
                    "\n" +
                    "    void createDialog(Context c) {\n" +
                    "        String str1 = \"something here\";\n" +
                    "        String str2 = \"another thing here\";\n" +
                    "        {\n" +
                    "            Evidence.apicalls(\"setTitle\", \"setMessage\");\n" +
                    "            Evidence.types(\"AlertDialog\");\n" +
                    "        }\n" +
                    "    }\n" +
                    "\n" +
                    "}";

    private static void synthesise(String code, Integer sampleCount) throws IOException, SynthesisError
    {
        List<String> results;
        {
            if(sampleCount != null)
                results = new ApiSynthesisClient("localhost", Configuration.ListenPort).synthesise(code, sampleCount);
            else
                results = new ApiSynthesisClient("localhost", Configuration.ListenPort).synthesise(code);
        }

        for(String result : results)
        {
	        System.out.println("\n---------- BEGIN PROGRAM  ----------");
            System.out.print(result);
        }
        System.out.print("\n"); // don't have next console prompt start on final line of code output.
    }

    public static void main(String[] args) throws IOException, SynthesisError
    {
        if(args.length >= 3)
        {
            System.out.println("usage: java edu.rice.pliny.programs.api_synthesis_server.ApiSynthesisLocalClient [file]");
            System.exit(0);
        }

        String code;
        if(args.length == 0)
        {
            code = _testDialog;
        }
        else
        {
            code = new String(Files.readAllBytes(Paths.get(args[0])));
        }

        Integer sampleCount;
        if(args.length == 2)
        {
            sampleCount = Integer.parseInt(args[1]);
        }
        else
        {
            sampleCount = null;
        }

        try
        {
            synthesise(code, sampleCount);
        }
        catch (ParseError e)
        {
            System.err.println(e.getMessage());
        }
    }
}
