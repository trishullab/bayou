package experiments.predict_asts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dsl.*;
import org.apache.commons.cli.*;
import synthesizer.RuntimeTypeAdapterFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ASTReader {

    CommandLine cmdLine;

    class JSONInputData {
        DSubTree original_ast;
        List<DSubTree> predicted_asts;
        List<List<String>> given_sequences;
        List<List<String>> unseen_sequences;
    }

    class JSONInputWrapper {
        List<JSONInputData> programs;
    }

    public ASTReader(String[] args) {
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
                .desc("metric to use: jaccard-seqs, jaccard-bag-api, num-stmts, binary-seqs")
                .build());
    }

    private List<JSONInputData> readData() throws IOException {
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
        JSONInputWrapper js = gson.fromJson(s, JSONInputWrapper.class);

        return js.programs;
    }

    public void execute() throws IOException {
        if (cmdLine == null)
            return;

        List<JSONInputData> data = readData();
        for (JSONInputData datapoint : data) {
            DSubTree originalAST = datapoint.original_ast;
            List<DSubTree> predictedASTs = datapoint.predicted_asts;
            List<Sequence> givenSequences = new ArrayList<>();
            for (List<String> calls : datapoint.given_sequences)
                givenSequences.add(new Sequence(calls));
            List<Sequence> unseenSequences = new ArrayList<>();
            for (List<String> calls : datapoint.unseen_sequences)
                unseenSequences.add(new Sequence(calls));

            MetricCalculator metric = null;
            if (cmdLine.getOptionValue("m").equals("jaccard-seqs"))
                metric = new JaccardSequencesMetric(predictedASTs, givenSequences, unseenSequences);
            else if (cmdLine.getOptionValue("m").equals("binary-seqs"))
                metric = new BinarySequencesMetric(predictedASTs, givenSequences, unseenSequences);
            else if (cmdLine.getOptionValue("m").equals("jaccard-bag-api"))
                metric = new JaccardAPICallsMetric(originalAST, predictedASTs);
            else if (cmdLine.getOptionValue("m").equals("num-stmts"))
                metric = new NumStatementsMetric(originalAST, predictedASTs);
            else if (cmdLine.getOptionValue("m").equals("num-control-structs"))
                metric = new NumControlStructuresMetric(originalAST, predictedASTs);
            else {
                System.err.println("invalid metric: " + cmdLine.getOptionValue("m"));
                System.exit(1);
            }
            metric.doCalculation();
        }
    }

    public static void main(String args[]) {
        try {
            new ASTReader(args).execute();
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

