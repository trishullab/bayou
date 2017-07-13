package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.*;
import org.apache.commons.cli.*;
import org.apache.commons.math3.stat.inference.TTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
                .desc("metric to use: equality-ast jaccard-sequences jaccard-api-calls num-statements num-control-structures")
                .build());
        opts.addOption(Option.builder("c")
                .longOpt("in-corpus")
                .hasArg()
                .numberOfArgs(1)
                .desc("consider 1:all programs (default), 2:only those in corpus, 3:only those NOT in corpus")
                .build());
        opts.addOption(Option.builder("t")
                .longOpt("top")
                .hasArg()
                .numberOfArgs(1)
                .desc("use the top-k ASTs only")
                .build());
        opts.addOption(Option.builder("a")
                .longOpt("aggregate")
                .hasArg()
                .numberOfArgs(1)
                .desc("aggregate metrics in each top-k: min (default), mean, stdv")
                .build());
        opts.addOption(Option.builder("p")
                .longOpt("p-value")
                .hasArg()
                .numberOfArgs(1)
                .desc("compute the p-value (Student's t-test) w.r.t. the provided DATA file")
                .build());
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
            case "jaccard-sequences":
                metric = new JaccardSequencesMetric();
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

        int inCorpus = cmdLine.hasOption("c")? Integer.parseInt(cmdLine.getOptionValue("c")): 1;
        String aggregate = cmdLine.hasOption("a")? cmdLine.getOptionValue("a"): "min";
        List<JSONInputFormat.DataPoint> data = JSONInputFormat.readData(cmdLine.getOptionValue("f"));
        if (inCorpus == 2)
            data = data.stream().filter(datapoint -> datapoint.in_corpus).collect(Collectors.toList());
        else if (inCorpus == 3)
            data = data.stream().filter(datapoint -> !datapoint.in_corpus).collect(Collectors.toList());

        List<Float> values = new ArrayList<>();
        for (JSONInputFormat.DataPoint datapoint : data) {
            DSubTree originalAST = datapoint.ast;
            List<DSubTree> predictedASTs = datapoint.out_asts.subList(0,
                    Math.min(topk, datapoint.out_asts.size()));

            values.add(metric.compute(originalAST, predictedASTs, aggregate));
        }

        List<Float> values2 = new ArrayList<>();
        if (cmdLine.hasOption("p")) {
            List<JSONInputFormat.DataPoint> data2 = JSONInputFormat.readData(cmdLine.getOptionValue("p"));
            if (inCorpus == 2)
                data2 = data2.stream().filter(datapoint -> datapoint.in_corpus).collect(Collectors.toList());
            else if (inCorpus == 3)
                data2 = data2.stream().filter(datapoint -> !datapoint.in_corpus).collect(Collectors.toList());

            for (JSONInputFormat.DataPoint datapoint : data2) {
                DSubTree originalAST = datapoint.ast;
                List<DSubTree> predictedASTs = datapoint.out_asts.subList(0,
                        Math.min(topk, datapoint.out_asts.size()));

                values2.add(metric.compute(originalAST, predictedASTs, aggregate));
            }
            if (values.size() != values2.size())
                throw new Error("DATA files do not match in size. Cannot compute p-value.");
        }

        float average = Metric.mean(values);
        float stdv = Metric.standardDeviation(values);

        if (cmdLine.hasOption("p")) {
            double[] dValues = values.stream().mapToDouble(v -> v.floatValue()).toArray();
            double[] dValues2 = values2.stream().mapToDouble(v -> v.floatValue()).toArray();
            double pValue = new TTest().pairedTTest(dValues, dValues2);
            System.out.println(String.format(
                    "%s (%d data points, each aggregated with %s): average=%f, stdv=%f, pvalue=%e",
                    m, data.size(), aggregate, average, stdv, pValue));
        }
        else
            System.out.println(String.format(
                    "%s (%d data points, each aggregated with %s): average=%f, stdv=%f",
                    m, data.size(), aggregate, average, stdv));
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

