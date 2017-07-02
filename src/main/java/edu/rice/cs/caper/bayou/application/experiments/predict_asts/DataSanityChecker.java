package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import edu.rice.cs.caper.bayou.core.dsl.*;
import edu.rice.cs.caper.bayou.core.synthesizer.RuntimeTypeAdapterFactory;
import org.apache.commons.cli.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class DataSanityChecker {

    CommandLine cmdLine;

    public DataSanityChecker(String[] args) {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

        try {
            this.cmdLine = parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("data_sanity_checker", clopts);
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
        opts.addOption(Option.builder("v")
                .longOpt("vocab-data")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("data file to use for vocabulary")
                .build());
        opts.addOption(Option.builder("t")
                .longOpt("top")
                .hasArg()
                .numberOfArgs(1)
                .desc("use the top-k ASTs only")
                .build());
        opts.addOption(Option.builder("o")
                .longOpt("output-file")
                .hasArg()
                .numberOfArgs(1)
                .desc("output file to print data to")
                .build());
    }

    public void execute() throws IOException {
        if (cmdLine == null)
            return;
        int topk = cmdLine.hasOption("t")? Integer.parseInt(cmdLine.getOptionValue("t")): 10;
        String file = cmdLine.getOptionValue("f");
        String vocabDataFile = cmdLine.getOptionValue("v");

        System.out.print("Gathering API call vocabulary...");
        Set<String> vocab = Collections.unmodifiableSet(getVocab(vocabDataFile));
        System.out.println("done");

        System.out.println(String.format("Checking validity of each AST in top-%d...", topk));
        JSONObject js = new JSONObject(new String(Files.readAllBytes(Paths.get(file))));
        JSONArray programs = js.getJSONArray("programs");
        RuntimeTypeAdapterFactory<DASTNode> nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode.class, "node")
                .registerSubtype(DAPICall.class)
                .registerSubtype(DBranch.class)
                .registerSubtype(DExcept.class)
                .registerSubtype(DLoop.class)
                .registerSubtype(DSubTree.class);
        Gson gsonIn = new GsonBuilder().registerTypeAdapterFactory(nodeAdapter).
                serializeNulls().create();

        int numASTs = 0, numValidASTs = 0, numInvalidASTs = 0;
        JSONInputFormat.Data data = new JSONInputFormat.Data();
        for (Object o: programs) {
            JSONObject program = (JSONObject) o;
            JSONArray predictASTs = program.getJSONArray("out_asts");

            JSONInputFormat.DataPoint datapoint = new JSONInputFormat.DataPoint();
            for (Object q: program.getJSONArray("apicalls"))
                datapoint.apicalls.add((String) q);
            for (Object q: program.getJSONArray("types"))
                datapoint.types.add((String) q);
            for (Object q: program.getJSONArray("context"))
                datapoint.context.add((String) q);
            datapoint.file = program.getString("file");
            datapoint.ast = gsonIn.fromJson(program.getJSONObject("ast").toString(), DSubTree.class);

            int i = 0;
            List<DSubTree> validASTs = new ArrayList<>();
            for (Object p: predictASTs) {
                if (i >= topk)
                    break;
                i++;
                JSONObject pAST = (JSONObject) p;
                DSubTree predictedAST;
                try {
                    predictedAST = gsonIn.fromJson(pAST.toString(), DSubTree.class);
                } catch (JsonSyntaxException e) {
                    numInvalidASTs += 1;
                    continue;
                }
                Set<String> apicalls = predictedAST.bagOfAPICalls().stream().map(c -> c.toString()).collect(Collectors.toSet());
                apicalls.removeAll(vocab);
                if (apicalls.isEmpty()) /* there's no API call that's not in vocab */
                    validASTs.add(predictedAST);
                else
                    numInvalidASTs += 1;
            }
            numValidASTs += validASTs.size();
            numASTs += i;

            datapoint.out_asts = validASTs;
            data.programs.add(datapoint);
        }

        System.out.println(String.format("Number of valid ASTs: %d/%d, invalid: %d/%d",
                numValidASTs, numASTs, numInvalidASTs, numASTs));

        if (cmdLine.hasOption("o")) {
            if (numInvalidASTs == 0) {
                System.out.println("Nothing to write.. No invalid ASTs");
                return;
            }
            String outfile = cmdLine.getOptionValue("o");
            System.out.print("Writing to file " + outfile + "...");
            writeToFile(data, outfile);
            System.out.println("done");
        }
    }

    void writeToFile(JSONInputFormat.Data data, String outfile) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(outfile));
        Gson outGson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        bw.write(outGson.toJson(data));
        bw.flush();
        bw.close();
    }

    static class VocabData {
        List<VocabDataPoint> programs;
    }

    static class VocabDataPoint {
        DSubTree ast;
    }

    public Set<String> getVocab(String vocabDataFile) throws IOException {
        RuntimeTypeAdapterFactory<DASTNode> nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode.class, "node")
                .registerSubtype(DAPICall.class)
                .registerSubtype(DBranch.class)
                .registerSubtype(DExcept.class)
                .registerSubtype(DLoop.class)
                .registerSubtype(DSubTree.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(nodeAdapter)
                .create();
        String s = new String(Files.readAllBytes(Paths.get(vocabDataFile)));
        VocabData js = gson.fromJson(s, VocabData.class);

        Set<String> vocab = new HashSet<>();
        for (VocabDataPoint dataPoint: js.programs) {
            DSubTree ast = dataPoint.ast;
            Set<DAPICall> apicalls = ast.bagOfAPICalls();
            vocab.addAll(apicalls.stream().map(c -> c.toString()).collect(Collectors.toSet()));
        }
        return vocab;
    }

    public static void main(String args[]) {
        try {
            new DataSanityChecker(args).execute();
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
