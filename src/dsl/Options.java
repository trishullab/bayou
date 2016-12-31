package dsl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Options {

    private void addOptions(org.apache.commons.cli.Options opts) {
        opts.addOption(Option.builder("f")
                .longOpt("input-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("input Java program")
                .build());

        opts.addOption(Option.builder("c")
                .longOpt("config-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("configuration JSON file")
                .build());

        opts.addOption(Option.builder("o")
                .longOpt("output-file")
                .hasArg()
                .numberOfArgs(1)
                .desc("output DSL AST to file")
                .build());
    }

    CommandLine cmdLine;
    JsonObject config;

    final List<String> API_CLASSES;
    final int NUM_UNROLLS;

    public Options(String[] args) throws ParseException, IOException {
        this.cmdLine = readCommandLine(args);
        this.config = readConfigFile(cmdLine.getOptionValue("config-file"));

        List<String> classes = new ArrayList<String>();
        for (JsonElement e : this.config.getAsJsonArray("api-classes"))
            classes.add(e.getAsString());
        this.API_CLASSES = Collections.unmodifiableList(classes);

        if (this.config.has("num-unrolls"))
            this.NUM_UNROLLS = this.config.getAsJsonPrimitive("num-unrolls").getAsInt();
        else
            this.NUM_UNROLLS = 1;
    }

    private CommandLine readCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

        try {
            return parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("driver", clopts);
            throw e;
        }
    }

    private JsonObject readConfigFile(String file) throws IOException {
        JsonParser parser = new JsonParser();
        File configFile = new File(file);

        return parser.parse(FileUtils.readFileToString(configFile, "utf-8")).getAsJsonObject();
    }
}
