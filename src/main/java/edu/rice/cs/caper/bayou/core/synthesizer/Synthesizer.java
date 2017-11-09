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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jface.text.Document;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Synthesizer {

    static ClassLoader classLoader;

    enum Mode {
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

    class JSONInputWrapper {
        List<DSubTree> asts;
    }

    public Synthesizer() {
        this.mode = Mode.COMBINATORIAL_ENUMERATOR; // default mode
    }

    public Synthesizer(Mode mode) {
        this.mode = mode;
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

    public List<String> execute(Parser parser, String astJson) {
        List<String> synthesizedPrograms = new LinkedList<>();
        List<DSubTree> asts = getASTsFromNN(astJson);

        classLoader = URLClassLoader.newInstance(parser.classpathURLs);

        CompilationUnit cu = parser.cu;
        List<String> programs = new ArrayList<>();
        for (DSubTree ast : asts) {
            Visitor visitor = new Visitor(ast, new Document(parser.source), cu, mode);
            try {
                cu.accept(visitor);
                if (visitor.synthesizedProgram == null)
                    continue;
                String program = visitor.synthesizedProgram.replaceAll("\\s", "");
                if (! programs.contains(program)) {
                    programs.add(program);
                    synthesizedPrograms.add(visitor.synthesizedProgram);
                }
            } catch (SynthesisException e) {
                // do nothing and try next ast
            }
        }

        return synthesizedPrograms;
    }
}
