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
package edu.rice.cs.caper.bayou.core.dom_driver;

import com.google.common.collect.Multiset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.core.dsl.DASTNode;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jdt.core.dom.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class Visitor extends ASTVisitor {

    public final CompilationUnit unit;
    public final Options options;
    private final JSONOutput _js;

    public List<MethodDeclaration> allMethods;

    // call stack during driver execution
    public final Stack<MethodDeclaration> callStack = new Stack<>();

    class JSONOutput {
        List<JSONOutputWrapper> programs;

        JSONOutput() {
            this.programs = new ArrayList<>();
        }
    }

    class JSONOutputWrapper {
        String file;
        DSubTree ast;
        List<Sequence> sequences;
        String javadoc;
        String method;

        public JSONOutputWrapper(DSubTree ast, List<Sequence> sequences, String javadoc, String methodName) {
            this.file = options.file;
            this.method = methodName;
            this.ast = ast;
            this.sequences = sequences;
            this.javadoc = javadoc;
        }
    }

    public Visitor(CompilationUnit unit, Options options) throws FileNotFoundException {
        this.unit = unit;
        this.options = options;

        _js = new JSONOutput();
        allMethods = new ArrayList<>();
    }

    @Override
    public boolean visit(TypeDeclaration clazz) {
        if (clazz.isInterface())
            return false;
        List<TypeDeclaration> classes = new ArrayList<>();
        classes.addAll(Arrays.asList(clazz.getTypes()));
        classes.add(clazz);

        for (TypeDeclaration cls : classes)
            allMethods.addAll(Arrays.asList(cls.getMethods()));
        List<MethodDeclaration> constructors = allMethods.stream().filter(m -> m.isConstructor()).collect(Collectors.toList());
        List<MethodDeclaration> publicMethods = allMethods.stream().filter(m -> !m.isConstructor() && Modifier.isPublic(m.getModifiers())).collect(Collectors.toList());

        // synchronized lists
        List<DSubTree> asts = new ArrayList<>();
        List<String> javaDocs = new ArrayList<>();
        List<String> methodNames = new ArrayList<>();

        if (!constructors.isEmpty() && !publicMethods.isEmpty()) {
            for (MethodDeclaration c : constructors)
                for (MethodDeclaration m : publicMethods) {
                    String javadoc = Utils.getJavadoc(m, options.JAVADOC_TYPE);
                    callStack.push(c);
                    DSubTree ast = new DOMMethodDeclaration(c, this).handle();
                    callStack.push(m);
                    ast.addNodes(new DOMMethodDeclaration(m, this).handle().getNodes());
                    callStack.pop();
                    callStack.pop();
                    if (ast.isValid()) {
                        asts.add(ast);
                        javaDocs.add(javadoc);
                        methodNames.add(m.getName().getIdentifier() + "@" + getLineNumber(m));
                    }
                }
        } else if (!constructors.isEmpty()) { // no public methods, only constructor
            for (MethodDeclaration c : constructors) {
                String javadoc = Utils.getJavadoc(c, options.JAVADOC_TYPE);
                callStack.push(c);
                DSubTree ast = new DOMMethodDeclaration(c, this).handle();
                callStack.pop();
                if (ast.isValid()) {
                    asts.add(ast);
                    javaDocs.add(javadoc);
                    methodNames.add(c.getName().getIdentifier() + "@" + getLineNumber(c));
                }
            }
        } else if (!publicMethods.isEmpty()) { // no constructors, methods executed typically through Android callbacks
            for (MethodDeclaration m : publicMethods) {
                String javadoc = Utils.getJavadoc(m, options.JAVADOC_TYPE);
                callStack.push(m);
                DSubTree ast = new DOMMethodDeclaration(m, this).handle();
                callStack.pop();
                if (ast.isValid()) {
                    asts.add(ast);
                    javaDocs.add(javadoc);
                    methodNames.add(m.getName().getIdentifier() + "@" + getLineNumber(m));
                }
            }
        }

        for (int i = 0; i < asts.size(); i++) {
            DSubTree ast = asts.get(i);
            String javaDoc = javaDocs.get(i);
            String methodName = methodNames.get(i);

            List<Sequence> sequences = new ArrayList<>();
            sequences.add(new Sequence());
            try {
                ast.updateSequences(sequences, options.MAX_SEQS, options.MAX_SEQ_LENGTH);
                List<Sequence> uniqSequences = new ArrayList<>(new HashSet<>(sequences));
                if (uniqSequences.size() > 0)
                    addToJson(ast, uniqSequences, javaDoc, methodName);
            } catch (DASTNode.TooManySequencesException e) {
                System.err.println("Too many sequences from AST");
            } catch (DASTNode.TooLongSequenceException e) {
                System.err.println("Too long sequence from AST");
            }
        }
        return false;
    }

    private void addToJson(DSubTree ast, List<Sequence> sequences, String javadoc, String methodName) {
        JSONOutputWrapper out = new JSONOutputWrapper(ast, sequences, javadoc, methodName);
        _js.programs.add(out);
    }

    public String buildJson() throws IOException {
        if (_js.programs.isEmpty())
            return null;

        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        return gson.toJson(_js);

    }

    public int getLineNumber(ASTNode node) {
        return unit.getLineNumber(node.getStartPosition());
    }
}