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
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

class ApiSynthesisLocalClient
{
    private static final String _testAddItemToList =
        "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                "import java.util.List;\n" +
                "\n" +
                "// Bayou supports two types of evidence:\n" +
                "// 1. apicalls - API methods the code should invoke\n" +
                "// 2. types - datatypes of objects which invoke API methods\n" +
                "\n" +
                "public class TestUtil {\n" +
                "\n" +
                "    // NOTE: Bayou only supports one synthesis task in a given\n" +
                "    // program at a time, so please comment out the rest.\n" +
                "\n" +
                "    /* Read from a file */\n" +
                "    void add(List<String> items, String item) {\n" +
                "        { // Provide evidence within a separate block\n" +
                "            /// call:add\n" +
                "        } // Synthesized code will replace this block\n" +
                "    }   \n" +
                "}";

    private static void synthesise(String code, Integer sampleCount, int maxProgramCount) throws IOException, SynthesisError
    {
        List<String> results;
        {
            ApiSynthesisClient client = new ApiSynthesisClient("localhost", Configuration.ListenPort.AsInt);
            if(sampleCount != null)
                results = client.synthesise(code, maxProgramCount, sampleCount);
            else
                results = client.synthesise(code, maxProgramCount);
        }

        for(String result : results)
        {
	        System.out.println("\n---------- BEGIN PROGRAM  ----------");
            System.out.print(result);
        }
        System.out.print("\n"); // don't have next console prompt start on final line of code output.
    }

    private static final String NUM_SAMPLES = "num_samples";

    private static final String NUM_PROGRAMS = "num_programs";

    private static final String HELP = "help";

    public static void main(String[] args) throws IOException, SynthesisError, ParseException
    {
        /*
         * Define the command line arguments for the application and parse args accordingly.
         */
        Options options = new Options();
        options.addOption("s", NUM_SAMPLES, true, "the number of asts to sample from the model");
        options.addOption("p", NUM_PROGRAMS, true, "the maximum number of programs to return");
        options.addOption(HELP, HELP, false, "print this message");

        CommandLine line = new DefaultParser().parse( options, args );

        /*
         * If more arguments are given than possibly correct or the user asked for help, show help message and exit.
         */
        if(args.length >= 6 || line.hasOption(HELP))
        {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "synthesize.sh [OPTION]... [FILE]", options);
            System.exit(1);
        }

        /*
         * Determine the query code to synthesize against.
         */
        String code;
        if(args.length == 0)
        {
            code = _testAddItemToList;
        }
        else
        {
            String finalArg = args[args.length-1];
            if(finalArg.startsWith("-"))
            {
                System.err.println("If command line arguments are specified, final argument must be a file path.");
                System.exit(2);
            }

            code = new String(Files.readAllBytes(Paths.get(finalArg)));
        }

        /*
         * Determine the model sample count requrest, or null if a default should be used.
         */
        Integer sampleCount = null;
        if(line.hasOption(NUM_SAMPLES) )
        {
            String numSamplesString = line.getOptionValue(NUM_SAMPLES);
            try
            {
                sampleCount = Integer.parseInt(numSamplesString);
                if(sampleCount < 1)
                    throw new NumberFormatException();
            }
            catch (NumberFormatException e)
            {
                System.err.println(NUM_SAMPLES + " must be a natural number.");
                System.exit(3);
            }
        }

        int maxProgramCount = Integer.MAX_VALUE;
        if(line.hasOption(NUM_PROGRAMS) )
        {
            String maxProgramCountStr = line.getOptionValue(NUM_PROGRAMS);
            try
            {
                maxProgramCount = Integer.parseInt(maxProgramCountStr);
                if(maxProgramCount < 1)
                    throw new NumberFormatException();
            }
            catch (NumberFormatException e)
            {
                System.err.println(NUM_PROGRAMS + " must be a natural number.");
                System.exit(4);
            }
        }

        try
        {
            synthesise(code, sampleCount, maxProgramCount);
        }
        catch (ParseError e)
        {
            System.err.println(e.getMessage());
        }
    }
}
