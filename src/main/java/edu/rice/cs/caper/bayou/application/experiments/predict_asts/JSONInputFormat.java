package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.List;
import java.util.Set;

public class JSONInputFormat {
    static class Data {
        List<DataPoint> programs;
    }

    static class DataPoint {
        DSubTree ast;
        List<DSubTree> out_asts;
        String file;
        Set<String> apicalls;
        Set<String> types;
        Set<String> context;
    }
}
