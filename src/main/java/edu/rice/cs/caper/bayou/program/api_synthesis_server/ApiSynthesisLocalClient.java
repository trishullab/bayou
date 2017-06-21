package edu.rice.cs.caper.bayou.program.api_synthesis_server;


import edu.rice.cs.caper.bayou.core.bayou_services_client.ap_synthesis.ApiSynthesisClient;
import edu.rice.cs.caper.bayou.core.bayou_services_client.ap_synthesis.SynthesisError;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class ApiSynthesisLocalClient
{
    private static final String _testIO = "import edu.rice.bayou.annotations.Evidence;\n" +
            "\n" +
            "public class TestIO1 {\n" +
            "\n" +
            "    @Evidence(apicalls = {\"readLine\", \"ready\"})\n" +
            "    void __bayou_fill(String file) {\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "}";

    private static void synthesise(String code) throws IOException, SynthesisError
    {
        for(String result : new ApiSynthesisClient("localhost", Configuration.ListenPort).synthesise(code))
        {
	        System.out.println("\n---------- BEGIN PROGRAM  ----------");
            System.out.print(result);
        }
    }

    public static void main(String[] args) throws IOException, SynthesisError
    {
        if(args.length == 0)
        {
            synthesise(_testIO);
        }
        else if(args.length == 1)
        {
            String code = new String(Files.readAllBytes(Paths.get(args[0])));
            synthesise(code);
        }
        else
        {
            System.out.println("usage: java edu.rice.pliny.programs.api_synthesis_server.ApiSynthesisLocalClient [file]");
        }
    }
}
