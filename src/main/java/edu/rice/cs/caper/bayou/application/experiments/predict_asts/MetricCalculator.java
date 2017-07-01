package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.core.dsl.*;
import edu.rice.cs.caper.bayou.core.synthesizer.RuntimeTypeAdapterFactory;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class MetricCalculator {

    CommandLine cmdLine;

    public MetricCalculator(String[] args) {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

        try {
            this.cmdLine = parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("predict_asts", clopts);
        }
    }

    private void addOptions(org.apache.commons.cli.Options opts) {
        opts.addOption(Option.builder("f")
                .longOpt("input-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("input file in JSON with original ASTs, predicted ASTs and sequences")
                .build());
        opts.addOption(Option.builder("m")
                .longOpt("metric")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("metric to use: equality-ast jaccard-api-calls num-statements num-control-structures")
                .build());
        opts.addOption(Option.builder("t")
                .longOpt("top")
                .hasArg()
                .numberOfArgs(1)
                .desc("use the top-k ASTs only")
                .build());
    }

    private List<JSONInputFormat.DataPoint> readData() throws IOException {
        RuntimeTypeAdapterFactory<DASTNode> nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode.class, "node")
                .registerSubtype(DAPICall.class)
                .registerSubtype(DBranch.class)
                .registerSubtype(DExcept.class)
                .registerSubtype(DLoop.class)
                .registerSubtype(DSubTree.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(nodeAdapter)
                .create();
        String s = new String(Files.readAllBytes(Paths.get(cmdLine.getOptionValue("f"))));
        JSONInputFormat.Data js = gson.fromJson(s, JSONInputFormat.Data.class);

        return js.programs;
    }

    public void execute() throws IOException {
        if (cmdLine == null)
            return;
        int topk = cmdLine.hasOption("t")? Integer.parseInt(cmdLine.getOptionValue("t")): 10;

        Metric metric;
        String m = cmdLine.getOptionValue("m");
        switch (m) {
            case "equality-ast":
                metric = new EqualityASTMetric();
                break;
            case "jaccard-api-calls":
                metric = new JaccardAPICallsMetric();
                break;
            case "num-control-structures":
                metric = new NumControlStructuresMetric();
                break;
            case "num-statements":
                metric = new NumStatementsMetric();
                break;
            default:
                System.err.println("invalid metric: " + cmdLine.getOptionValue("m"));
                return;
        }

        List<JSONInputFormat.DataPoint> data = readData();
        float value = 0;
        for (JSONInputFormat.DataPoint datapoint : data) {
            DSubTree originalAST = datapoint.ast;
            List<DSubTree> predictedASTs = datapoint.out_asts.subList(0,
                    Math.min(topk, datapoint.out_asts.size()));

            value += metric.compute(originalAST, predictedASTs);
        }
        value /= data.size();
        System.out.println(String.format(
                "Average value of metric %s across %d data points: %f",
                m, data.size(), value));
    }

    public static void main(String args[]) {
        try {
            new MetricCalculator(args).execute();
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

