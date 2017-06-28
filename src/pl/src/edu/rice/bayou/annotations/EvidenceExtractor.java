package edu.rice.bayou.annotations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EvidenceExtractor extends ASTVisitor {

    CommandLine cmdLine;

    class JSONOutputWrapper {
        List<String> apicalls;
        List<String> types;
        List<String> context;

        public JSONOutputWrapper() {
            this.apicalls = new ArrayList<>();
            this.types = new ArrayList<>();
            this.context = new ArrayList<>();
        }
    }

    JSONOutputWrapper output;

    public EvidenceExtractor(String[] args) {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

        try {
            this.cmdLine = parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("edu/rice/bayou/evidence_extractor", clopts);
        }

        this.output = new JSONOutputWrapper();
    }

    private void addOptions(org.apache.commons.cli.Options opts) {
        opts.addOption(Option.builder("f")
                .longOpt("input-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("input Java program")
                .build());
    }

    public void execute() throws IOException {
        if (cmdLine == null)
            return;

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        File input = new File(cmdLine.getOptionValue("input-file"));
        String classpath = System.getenv("CLASSPATH");

        String source = FileUtils.readFileToString(input, "utf-8");
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { classpath != null? classpath : "" },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            cu.accept(this);
            System.out.println(gson.toJson(output));
        } catch (Exception e) {
            System.err.println("Unexpected error occurred: " + e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public boolean visit(MethodInvocation invocation) {
        IMethodBinding binding = invocation.resolveMethodBinding();
        if (binding == null)
            throw new RuntimeException("Could not resolve binding. " +
                "Either CLASSPATH is not set correctly, or there is an invalid evidence type.");

        ITypeBinding cls = binding.getDeclaringClass();
        if (cls == null || !cls.getQualifiedName().equals("edu.rice.bayou.annotations.Evidence"))
            return false;

        // performing casts wildly.. if any exceptions occur it's due to incorrect input format
        if (binding.getName().equals("apicalls")) {
            for (Object arg : invocation.arguments()) {
                StringLiteral a = (StringLiteral) arg;
                output.apicalls.add(a.getLiteralValue());
            }
        } else if (binding.getName().equals("types")) {
            for (Object arg : invocation.arguments()) {
                StringLiteral a = (StringLiteral) arg;
                output.types.add(a.getLiteralValue());
            }
        } else if (binding.getName().equals("context")) {
            for (Object arg : invocation.arguments()) {
                StringLiteral a = (StringLiteral) arg;
                output.context.add(a.getLiteralValue());
            }
        } else throw new RuntimeException("Invalid evidence type: " + binding.getName());

        return false;
    }

    public static void main(String args[]) {
        try {
            new EvidenceExtractor(args).execute();
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
