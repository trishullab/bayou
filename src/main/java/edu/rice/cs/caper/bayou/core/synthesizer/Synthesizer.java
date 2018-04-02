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

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.core.dsl.*;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Synthesizer {

    static ClassLoader classLoader;

    public enum Mode {
        COMBINATORIAL_ENUMERATOR,
        CONDITIONAL_PROGRAM_GENERATOR,
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    Mode mode;

    class JSONInput {
        DSubTree ast;
        float probability;
    }

    class JSONInputWrapper {
        List<JSONInput> asts;
    }

    public Synthesizer() {
        this.mode = Mode.CONDITIONAL_PROGRAM_GENERATOR; // default mode
    }

    public Synthesizer(Mode mode) {
        this.mode = mode;
    }

    private List<JSONInput> getASTsFromNN(String astJson) {
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

    public List<String> execute(Parser parser, String astJson) {
        List<String> synthesizedPrograms = new LinkedList<>();
        List<JSONInput> asts = getASTsFromNN(astJson);

        classLoader = URLClassLoader.newInstance(parser.classpathURLs);

        CompilationUnit cu = parser.cu;
        List<String> programs = new ArrayList<>();
        for (JSONInput ast : asts) {
            Visitor visitor = new Visitor(ast.ast, new Document(parser.source), cu, mode);
            try {
                cu.accept(visitor);
                if (visitor.synthesizedProgram == null)
                    continue;
                String program = visitor.synthesizedProgram.replaceAll("\\s", "");
                if (! programs.contains(program)) {
                    String formattedProgram = new Formatter().formatSource(visitor.synthesizedProgram);
                    programs.add(program);
                    synthesizedPrograms.add(formattedProgram);
                }
            } catch (SynthesisException|FormatterException e) {
                // do nothing and try next ast
            }
        }

        return synthesizedPrograms;
    }
}
