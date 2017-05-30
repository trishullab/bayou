package edu.rice.cs.caper.lib.bayou.synthesizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.lib.bayou.dsl.*;
import org.apache.commons.cli.*;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;

import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public class Synthesizer {

    private final PrintStream _out;

   // CommandLine cmdLine;

    static ClassLoader classLoader;

    class JSONInputWrapper {
        List<DSubTree> asts;
    }

    public Synthesizer(String[] args)
    {
        this(System.out);
    }

    public Synthesizer(PrintStream outStream)
    {
        _out = outStream;
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

//        try {
//            this.cmdLine = parser.parse(clopts, args);
//        } catch (ParseException e) {
//            HelpFormatter help = new HelpFormatter();
//            help.printHelp("edu/rice/bayou/synthesizer", clopts);
//        }
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

    private List<DSubTree> getASTsFromNN(String astJson) {
        RuntimeTypeAdapterFactory<DASTNode> nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode.class, "node")
                .registerSubtype(DAPICall.class)
                .registerSubtype(DBranch.class)
                .registerSubtype(DExcept.class)
                .registerSubtype(DLoop.class)
                .registerSubtype(DSubTree.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(nodeAdapter)
                .create();
       // String s = new String(Files.readAllBytes(Paths.get(cmdLine.getOptionValue("a"))));
        JSONInputWrapper js = gson.fromJson(astJson, JSONInputWrapper.class);

        return js.asts;
    }

    public void execute(String source, String astJson, String classpath) throws IOException {
       // if (cmdLine == null)
        //    return;

        ASTParser parser = ASTParser.newParser(AST.JLS8);
     //   File input = new File(cmdLine.getOptionValue("input-file"));
        classpath = classpath == null? "" : classpath;

       // String source = FileUtils.readFileToString(input, "utf-8");
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { classpath },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        List<DSubTree> asts = getASTsFromNN(astJson);

        List<URL> urlList = new ArrayList<>();
        for (String cp : classpath.split(":"))
            urlList.add(new URL("jar:file:" + cp + "!/"));
        URL[] urls = urlList.toArray(new URL[0]);
        classLoader = URLClassLoader.newInstance(urls);

        List<String> programs = new ArrayList<>();
        for (DSubTree ast : asts) {
            Visitor visitor = new Visitor(ast, new Document(source), cu);
            try {
                cu.accept(visitor);
                String program = visitor.synthesizedProgram.replaceAll("\\s", "");
                if (! programs.contains(program)) {
                    programs.add(program);
                    _out.println(visitor.synthesizedProgram);
                    _out.println("/* --- End of program --- */\n\n");
                }
            }
            catch (Exception e)
            {
                // do nothing and try next ast
            }
        }
    }

//    public static void main(String args[]) {
//        try {
//            new Synthesizer(args).execute();
//        } catch (IOException e) {
//            System.err.println("Error occurred: " + e.getMessage());
//            e.printStackTrace();
//            System.exit(1);
//        }
//    }
}
