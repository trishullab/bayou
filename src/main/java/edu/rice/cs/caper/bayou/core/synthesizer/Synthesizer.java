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
package edu.rice.cs.caper.bayou.core.synthesizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.core.dsl.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Synthesizer {

    /**
     * Place to send application logging information.
     */
    private static final Logger _logger = LogManager.getLogger(Synthesizer.class.getName());

    static ClassLoader classLoader;

    class JSONInputWrapper {
        List<DSubTree> asts;
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
        JSONInputWrapper js = gson.fromJson(astJson, JSONInputWrapper.class);

        return js.asts;
    }

    public List<String> execute(String source, String astJson, String classpath) throws IOException, ParseException {

        List<String> synthesizedPrograms = new LinkedList<>();

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        classpath = classpath == null? "" : classpath;

        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { classpath },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        List<IProblem> problems = Arrays.stream(cu.getProblems()).filter(p ->
                p.isError() &&
                        p.getID() != IProblem.PublicClassMustMatchFileName && // we use "Program.java"
                        p.getID() != IProblem.ParameterMismatch // Evidence varargs
        ).collect(Collectors.toList());
        if (problems.size() > 0)
            throw new ParseException(problems);

        List<DSubTree> asts = getASTsFromNN(astJson);

        List<URL> urlList = new ArrayList<>();
        for (String cp : classpath.split(File.pathSeparator)) {
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
            } catch (Exception e) {
                // do nothing and try next ast
            }
        }

        return synthesizedPrograms;
    }

}
