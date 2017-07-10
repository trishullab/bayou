package edu.rice.cs.caper.bayou.core.synthesizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.core.dsl.*;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Synthesizer {

    /**
     * Place to send application logging information.
     */
    private static final Logger _logger = LogManager.getLogger(Synthesizer.class.getName());

    static ClassLoader classLoader;

    class JSONInputWrapper {
        List<DSubTree> asts;
    }

    public Synthesizer()
    {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

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

    public List<String> execute(String source, String astJson, String classpath) throws IOException {

        List<String> synthesizedPrograms = new LinkedList<>();

        ASTParser parser = ASTParser.newParser(AST.JLS8);
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
        for (String cp : classpath.split(File.pathSeparator))
        {
            _logger.trace("cp: " + cp);
            urlList.add(new URL("jar:file:" + cp + "!/"));
        }
        URL[] urls = urlList.toArray(new URL[0]);

        classLoader = URLClassLoader.newInstance(urls);


        List<String> programs = new ArrayList<>();
        for (DSubTree ast : asts) {
            Visitor visitor = new Visitor(ast, new Document(source), cu);
            try {
                cu.accept(visitor);
                if (visitor.synthesizedProgram == null)
                    continue;
                String program = visitor.synthesizedProgram.replaceAll("\\s", "");
                if (! programs.contains(program)) {
                    programs.add(program);
                    synthesizedPrograms.add(visitor.synthesizedProgram);
                }
            }
            catch (Exception e)
            {
                // do nothing and try next ast
            }
        }

        return synthesizedPrograms;
    }

}
