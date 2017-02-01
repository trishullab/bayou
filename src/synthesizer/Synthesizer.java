package synthesizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dsl.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Synthesizer {

    CommandLine cmdLine;

    static ClassLoader classLoader;

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
                .desc("input Java program")
                .build());
        opts.addOption(Option.builder("a")
                .longOpt("asts-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("file containing ASTs from NN (in JSON)")
                .build());
    }

    private List<DSubTree> getASTsFromNN() throws IOException {
        RuntimeTypeAdapterFactory<DASTNode> nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode.class, "node")
                .registerSubtype(DAPICall.class)
                .registerSubtype(DBranch.class)
                .registerSubtype(DExcept.class)
                .registerSubtype(DLoop.class)
                .registerSubtype(DSubTree.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(nodeAdapter)
                .create();
        String s = new String(Files.readAllBytes(Paths.get(cmdLine.getOptionValue("a"))));
        JSONInputWrapper js = gson.fromJson(s, JSONInputWrapper.class);

        return js.asts;
    }

    public void execute() throws IOException {
        if (cmdLine == null)
            return;

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        File input = new File(cmdLine.getOptionValue("input-file"));

        String source = FileUtils.readFileToString(input, "utf-8");
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { System.getenv("CLASSPATH") },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        List<DSubTree> asts = getASTsFromNN();

        URL[] urls = { new URL("jar:file:" + System.getenv("CLASSPATH") + "!/") };
        classLoader = URLClassLoader.newInstance(urls);

        for (DSubTree ast : asts) {
            Visitor visitor = new Visitor(ast, new Document(source), cu);
            cu.accept(visitor);
            System.out.println(visitor.synthesizedProgram);
        }
    }

    public static void main(String args[]) {
        try {
            new Synthesizer(args).execute();
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
