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
package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.Expose;
import edu.rice.cs.caper.bayou.application.dom_driver.*;
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
        RuntimeTypeAdapterFactory<DOMExpression> exprAdapter =
                RuntimeTypeAdapterFactory.of(DOMExpression.class, "node")
                        .registerSubtype(DOMMethodInvocation.class)
                        .registerSubtype(DOMClassInstanceCreation.class)
                        .registerSubtype(DOMInfixExpression.class)
                        .registerSubtype(DOMPrefixExpression.class)
                        .registerSubtype(DOMConditionalExpression.class)
                        .registerSubtype(DOMVariableDeclarationExpression.class)
                        .registerSubtype(DOMAssignment.class)
                        .registerSubtype(DOMParenthesizedExpression.class)
                        .registerSubtype(DOMNullLiteral.class)
                        .registerSubtype(DOMName.class)
                        .registerSubtype(DOMNumberLiteral.class);
        RuntimeTypeAdapterFactory<DOMStatement> stmtAdapter =
                RuntimeTypeAdapterFactory.of(DOMStatement.class, "node")
                        .registerSubtype(DOMBlock.class)
                        .registerSubtype(DOMExpressionStatement.class)
                        .registerSubtype(DOMIfStatement.class)
                        .registerSubtype(DOMSwitchStatement.class)
                        .registerSubtype(DOMSwitchCase.class)
                        .registerSubtype(DOMDoStatement.class)
                        .registerSubtype(DOMForStatement.class)
                        .registerSubtype(DOMEnhancedForStatement.class)
                        .registerSubtype(DOMWhileStatement.class)
                        .registerSubtype(DOMTryStatement.class)
                        .registerSubtype(DOMVariableDeclarationStatement.class)
                        .registerSubtype(DOMSynchronizedStatement.class)
                        .registerSubtype(DOMReturnStatement.class)
                        .registerSubtype(DOMLabeledStatement.class);
        Gson gsonIn = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(exprAdapter)
                .registerTypeAdapterFactory(stmtAdapter)
                .excludeFieldsWithoutExposeAnnotation()
                .create();

        int numASTs = 0, numValidASTs = 0, numInvalidASTs = 0;
        JSONInputFormat.Data data = new JSONInputFormat.Data();
        for (Object o: programs) {
            JSONObject program = (JSONObject) o;
            JSONArray predictASTs = program.getJSONArray("out_aml_asts");

            JSONInputFormat.DataPoint datapoint = new JSONInputFormat.DataPoint();
            for (Object q: program.getJSONArray("apicalls"))
                datapoint.apicalls.add((String) q);
            for (Object q: program.getJSONArray("types"))
                datapoint.types.add((String) q);
            for (Object q: program.getJSONArray("context"))
                datapoint.context.add((String) q);
            for (Object q: program.getJSONArray("keywords"))
                datapoint.keywords.add((String) q);
            datapoint.file = program.has("file")? program.getString("file") : null;
            datapoint.aml = program.getString("aml");
            datapoint.aml_ast = gsonIn.fromJson(program.getJSONObject("aml_ast").toString(), DOMMethodDeclaration.class);

            int i = 0;
            List<DOMMethodDeclaration> validASTs = new ArrayList<>();
            for (Object p: predictASTs) {
                if (i >= topk)
                    break;
                i++;
                JSONObject pAST = (JSONObject) p;
                DOMMethodDeclaration predictedAST;
                try {
                    predictedAST = gsonIn.fromJson(pAST.toString(), DOMMethodDeclaration.class);
                } catch (JsonParseException e) {
                    numInvalidASTs += 1;
                    continue;
                }
                Set<String> apicalls;
                try {
                    apicalls = predictedAST.bagOfAPICalls().stream().map(c -> c.toString()).collect(Collectors.toSet());
                } catch (RuntimeException e) {
                    numInvalidASTs += 1;
                    continue;
                }
                apicalls.removeAll(vocab);
                if (apicalls.isEmpty()) /* there's no API call that's not in vocab */
                    validASTs.add(predictedAST);
                else
                    numInvalidASTs += 1;
            }
            numValidASTs += validASTs.size();
            numASTs += i;

            datapoint.out_aml_asts = validASTs;
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
        Gson outGson = new GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().serializeNulls().create();
        bw.write(outGson.toJson(data));
        bw.flush();
        bw.close();
    }

    static class VocabData {
        @Expose
        List<VocabDataPoint> programs;
    }

    static class VocabDataPoint {
        @Expose
        DOMMethodDeclaration aml_ast;
    }

    public Set<String> getVocab(String vocabDataFile) throws IOException {
        RuntimeTypeAdapterFactory<DOMExpression> exprAdapter =
                RuntimeTypeAdapterFactory.of(DOMExpression.class, "node")
                        .registerSubtype(DOMMethodInvocation.class)
                        .registerSubtype(DOMClassInstanceCreation.class)
                        .registerSubtype(DOMInfixExpression.class)
                        .registerSubtype(DOMPrefixExpression.class)
                        .registerSubtype(DOMConditionalExpression.class)
                        .registerSubtype(DOMVariableDeclarationExpression.class)
                        .registerSubtype(DOMAssignment.class)
                        .registerSubtype(DOMParenthesizedExpression.class)
                        .registerSubtype(DOMNullLiteral.class)
                        .registerSubtype(DOMName.class)
                        .registerSubtype(DOMNumberLiteral.class);
        RuntimeTypeAdapterFactory<DOMStatement> stmtAdapter =
                RuntimeTypeAdapterFactory.of(DOMStatement.class, "node")
                        .registerSubtype(DOMBlock.class)
                        .registerSubtype(DOMExpressionStatement.class)
                        .registerSubtype(DOMIfStatement.class)
                        .registerSubtype(DOMSwitchStatement.class)
                        .registerSubtype(DOMSwitchCase.class)
                        .registerSubtype(DOMDoStatement.class)
                        .registerSubtype(DOMForStatement.class)
                        .registerSubtype(DOMEnhancedForStatement.class)
                        .registerSubtype(DOMWhileStatement.class)
                        .registerSubtype(DOMTryStatement.class)
                        .registerSubtype(DOMVariableDeclarationStatement.class)
                        .registerSubtype(DOMSynchronizedStatement.class)
                        .registerSubtype(DOMReturnStatement.class)
                        .registerSubtype(DOMLabeledStatement.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(exprAdapter)
                .registerTypeAdapterFactory(stmtAdapter)
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        String s = new String(Files.readAllBytes(Paths.get(vocabDataFile)));
        VocabData js = gson.fromJson(s, VocabData.class);

        Set<String> vocab = new HashSet<>();
        for (VocabDataPoint dataPoint: js.programs) {
            DOMMethodDeclaration aml_ast = dataPoint.aml_ast;
            Set<String> apicalls = aml_ast.bagOfAPICalls();
            vocab.addAll(apicalls);
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
