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
package edu.rice.cs.caper.bayou.application.ast_quality_perf_test;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
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
        Options clopts = new Options();

        addOptions(clopts);

        try {
            this.cmdLine = parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("ast_quality_perf_test", clopts);
        }
    }

    private void addOptions(Options opts) {
        opts.addOption(Option.builder("f")
                .longOpt("input-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("input file in JSON with original ASTs and predicted ASTs")
                .build());
        opts.addOption(Option.builder("m")
                .longOpt("metric")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("metric to use: equality-ast jaccard-sequences jaccard-api-calls num-statements num-control-structures")
                .build());
        opts.addOption(Option.builder("t")
                .longOpt("top")
                .hasArg()
                .numberOfArgs(1)
                .desc("use the top-k ASTs only (default: 3)")
                .build());
    }

    public void execute() throws IOException {
        if (cmdLine == null)
            return;
        int topk = cmdLine.hasOption("t")? Integer.parseInt(cmdLine.getOptionValue("t")): 3;

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
            case "latency":
                metric = null;
                break;
            default:
                System.err.println("invalid metric: " + cmdLine.getOptionValue("m"));
                return;
        }

        List<JSONInputFormat.DataPoint> data = JSONInputFormat.readData(cmdLine.getOptionValue("f"));

        List<Float> values = new ArrayList<>();
	int dataIndex = 0;
        for (JSONInputFormat.DataPoint datapoint : data) {
            DSubTree originalAST = datapoint.ast;
            List<DSubTree> predictedASTs = datapoint.out_asts.subList(0,
                    Math.min(topk, datapoint.out_asts.size()));

            if (m.equals("latency"))
                values.add(datapoint.latency);
            else {
                float metricValue = metric.compute(originalAST, predictedASTs);
                if (m.equals("equality-ast") && metricValue >= 1) {
                    System.out.println("program-index=" + dataIndex + ", metric=equality-test, predicted-index=" + (metricValue - 1));
		    metricValue = metricValue >= 1 ? 1 : 0;
                }	
                values.add(metricValue);
            }
            dataIndex++;
	}

        float average = Metric.mean(values);
        float stdv = Metric.standardDeviation(values);

        System.out.println(String.format("%.2f,%.2f", average, stdv));
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

