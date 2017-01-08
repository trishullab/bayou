package pprinter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dsl.*;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class PrettyPrinter {

    CommandLine cmdLine;

    class JSONInputWrapper {
        List<DBlock> asts;
    }

    public PrettyPrinter(String[] args) {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

        try {
            this.cmdLine = parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("pretty_print", clopts);
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

        RuntimeTypeAdapterFactory<DStatement> stmtFactory = RuntimeTypeAdapterFactory.of(DStatement.class, "node")
                .registerSubtype(DBlock.class)
                .registerSubtype(DExpressionStatement.class)
                .registerSubtype(DIfStatement.class)
                .registerSubtype(DTryStatement.class)
                .registerSubtype(DVariableDeclarationStatement.class)
                .registerSubtype(DWhileStatement.class);

        RuntimeTypeAdapterFactory<DExpression> exprFactory = RuntimeTypeAdapterFactory.of(DExpression.class, "node")
                .registerSubtype(DName.class)
                .registerSubtype(DNullLiteral.class)
                .registerSubtype(DMethodInvocation.class)
                .registerSubtype(DClassInstanceCreation.class)
                .registerSubtype(DInfixExpression.class)
                .registerSubtype(DAssignment.class)
                .registerSubtype(DParenthesizedExpression.class);

        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(stmtFactory)
                .registerTypeAdapterFactory(exprFactory)
                .create();

        JSONInputWrapper js = gson.fromJson(s, JSONInputWrapper.class);
        for (DBlock ast : js.asts)
            System.out.println(ast.sketch() + "\n");
    }

    public static void main(String args[]) {
        new PrettyPrinter(args).run();
    }
}
