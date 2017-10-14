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
package edu.rice.cs.caper.bayou.application.experiments.program_generation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.core.synthesizer.ParseException;
import edu.rice.cs.caper.bayou.core.synthesizer.Parser;
import edu.rice.cs.caper.bayou.core.synthesizer.Synthesizer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SketchToAMLCompiler {

    String source =
                    "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                    "public class Test {\n" +
                    "    void toSynthesize(String userInput) {\n" +
                    "        {\n" +
                    "            Evidence.apicalls(\"this is just a marker\");\n" +
                    "        }\n" +
                    "    }\n" +
                    "}\n";

    List<String> compile(String sketches) throws ParseException {
        Parser parser = new Parser(source, System.getProperty("java.class.path"));
        parser.parse();

        Synthesizer synthesizer = new Synthesizer();
        return synthesizer.execute(parser, sketches);
    }

    void compileData(String datafile, String outfile) throws IOException, ParseException {
        List<JSONInputFormat.DataPoint> programs = JSONInputFormat.readData(datafile);
        List<String> amlPrograms = new ArrayList<>();
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        int i = 0;
        for (JSONInputFormat.DataPoint program : programs) {
            List<String> output;
            i++;
            try {
                output = compile("{ \"asts\": [" + gson.toJson(program.ast) + "]}");
                if (output.size() != 1)
                    amlPrograms.add("ERROR");
                else
                    amlPrograms.add(output.get(0));
                System.out.println(String.format("done %d/%d", i, programs.size()));
            } catch (RuntimeException e) {
                amlPrograms.add("ERROR");
                System.out.println(String.format("ERROR %d/%d", i, programs.size()));
            }
        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
        bw.write("{ \"programs\": [\n" + gson.toJson(amlPrograms) + "\n]}");
        bw.close();
    }

    public static void main(String args[]) throws IOException, ParseException {
        if (args.length != 2) {
            System.err.println("Usage: sketchToAML DATA.json DATA-output.json");
            System.exit(1);
        }
        new SketchToAMLCompiler().compileData(args[0], args[1]);
    }
}
