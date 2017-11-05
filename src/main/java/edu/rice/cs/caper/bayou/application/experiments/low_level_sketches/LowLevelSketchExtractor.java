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
package edu.rice.cs.caper.bayou.application.experiments.low_level_sketches;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import edu.rice.cs.caper.bayou.core.synthesizer.RuntimeTypeAdapterFactory;


import java.io.*;
import java.util.List;

public class LowLevelSketchExtractor {

    class JSONInputWrapper {
        String file;
        DSubTreeLowLevel ast;
        List<Sequence> sequences;
        List<String> apicalls, types;
    }

    class JsonOutputWrapper {
        String file;
        DSubTreeLowLevel ast;
        String low_level_sketch;
        List<Sequence> sequences;
        List<String> apicalls, types;
    }

    File inFile, outFile;

    public LowLevelSketchExtractor(String input, String output) {
        this.inFile = new File(input);
        this.outFile = new File(output);
    }

    public void execute() throws IOException {
        RuntimeTypeAdapterFactory<DASTNodeLowLevel> nodeAdapter =
                RuntimeTypeAdapterFactory.of(DASTNodeLowLevel.class, "node")
                        .registerSubtype(DAPICallLowLevel.class, "DAPICall")
                        .registerSubtype(DBranchLowLevel.class, "DBranch")
                        .registerSubtype(DExceptLowLevel.class, "DExcept")
                        .registerSubtype(DLoopLowLevel.class, "DLoop")
                        .registerSubtype(DSubTreeLowLevel.class, "DSubTree");
        Gson gsonIn = new GsonBuilder().registerTypeAdapterFactory(nodeAdapter).
                serializeNulls().create();
        JsonReader reader = new JsonReader(new FileReader(inFile));

        Gson gsonOut = new GsonBuilder().serializeNulls().create();
        JsonWriter writer = new JsonWriter(new FileWriter(outFile));

        reader.beginObject();
        reader.nextName();
        reader.beginArray();

        writer.setIndent("  ");
        writer.beginObject();
        writer.name("programs");
        writer.beginArray();

        System.out.println();
        for (int i = 0; reader.hasNext(); i++) {
            System.out.print(String.format("\rProcessed %s programs", i));
            JSONInputWrapper inputProgram = gsonIn.fromJson(reader, JSONInputWrapper.class);
            JsonOutputWrapper outputProgram = new JsonOutputWrapper();

            outputProgram.file = inputProgram.file;
            outputProgram.ast = inputProgram.ast;
            outputProgram.low_level_sketch = inputProgram.ast.getLowLevelSketch();
            outputProgram.sequences = inputProgram.sequences;
            outputProgram.apicalls = inputProgram.apicalls;
            outputProgram.types = inputProgram.types;

            gsonOut.toJson(outputProgram, JsonOutputWrapper.class, writer);
        }
        System.out.println();

        reader.close();
        writer.endArray();
        writer.endObject();
        writer.close();
    }

    static void usage() {
        System.out.println("Usage: extractor DATA-input.json DATA-output.json");
    }

    public static void main(String args[]) {
        if (args.length == 2) {
            try {
                new LowLevelSketchExtractor(args[0], args[1]).execute();
            } catch (IOException e) {
                System.err.println("Unexpected exception: " + e.getMessage());
                System.exit(1);
            }
        }
        else
            usage();
    }
}
