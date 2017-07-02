package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.core.dsl.*;
import edu.rice.cs.caper.bayou.core.synthesizer.RuntimeTypeAdapterFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JSONInputFormat {

    public static List<DataPoint> readData(String file) throws IOException {
        RuntimeTypeAdapterFactory<DASTNode> nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode.class, "node")
                .registerSubtype(DAPICall.class)
                .registerSubtype(DBranch.class)
                .registerSubtype(DExcept.class)
                .registerSubtype(DLoop.class)
                .registerSubtype(DSubTree.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(nodeAdapter)
                .create();
        String s = new String(Files.readAllBytes(Paths.get(file)));
        Data js = gson.fromJson(s, Data.class);

        return js.programs;
    }

    static class Data {
        List<DataPoint> programs;
        Data() {
            programs = new ArrayList<>();
        }
    }

    static class DataPoint {
        DSubTree ast;
        List<DSubTree> out_asts;
        String file;
        Set<String> apicalls;
        Set<String> types;
        Set<String> context;

        DataPoint() {
            out_asts = new ArrayList<>();
            apicalls = new HashSet<>();
            types = new HashSet<>();
            context = new HashSet<>();
        }
    }
}
