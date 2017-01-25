package synthesizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dsl.*;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Synthesizer {

    CommandLine cmdLine;

    class JSONInputWrapper {
        List<DSubTree> asts;
    }

    public Synthesizer(String[] args) {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

        try {
            this.cmdLine = parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("synthesizer", clopts);
        }
    }

    private void addOptions(org.apache.commons.cli.Options opts) {
        opts.addOption(Option.builder("f")
                .longOpt("input-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("input ASTs in JSON")
                .build());
    }

    public void run() {
        if (cmdLine == null)
            return;

        String s;
        try {
            s = new String(Files.readAllBytes(Paths.get(cmdLine.getOptionValue("f"))));
        } catch (IOException e) {
            System.out.println("File " + cmdLine.getOptionValue("f") + " not found");
            return;
        }

        RuntimeTypeAdapterFactory<DASTNode> nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode.class, "node")
                .registerSubtype(DAPICall.class)
                .registerSubtype(DBranch.class)
                .registerSubtype(DExcept.class)
                .registerSubtype(DLoop.class)
                .registerSubtype(DSubTree.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(nodeAdapter)
                .create();

        JSONInputWrapper js = gson.fromJson(s, JSONInputWrapper.class);
        for (DSubTree ast : js.asts)
            System.out.println(ast.getNodes().size());
    }

    public static void main(String args[]) {
        new Synthesizer(args).run();
    }
}
