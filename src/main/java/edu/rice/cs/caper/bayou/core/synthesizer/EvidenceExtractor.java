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
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class EvidenceExtractor extends ASTVisitor {

    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(EvidenceExtractor.class.getName());

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

    JSONOutputWrapper output = new JSONOutputWrapper();
    Block evidenceBlock;


    public String execute(String source, String classpath) throws ParseException {

        _logger.trace("source");
        _logger.trace("classpath:" + classpath);

        ASTParser parser = ASTParser.newParser(AST.JLS8);


        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { classpath != null? classpath : "" },
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

        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        cu.accept(this);
        return gson.toJson(output);
    }

    @Override
    public boolean visit(MethodInvocation invocation) {
        IMethodBinding binding = invocation.resolveMethodBinding();
        if (binding == null)
            throw new RuntimeException("Could not resolve binding. " +
                "Either CLASSPATH is not set correctly, or there is an invalid evidence type.");

        ITypeBinding cls = binding.getDeclaringClass();
        if (cls == null || !cls.getQualifiedName().equals("edu.rice.cs.caper.bayou.annotations.Evidence"))
            return false;

        if (! (invocation.getParent().getParent() instanceof Block))
            throw new RuntimeException("Evidence has to be given in a (empty) block.");
        Block evidenceBlock = (Block) invocation.getParent().getParent();

        if (!isLegalEvidenceBlock(evidenceBlock))
            throw new RuntimeException("Evidence API calls should not be mixed with other program statements.");

        if (this.evidenceBlock != null && this.evidenceBlock != evidenceBlock)
            throw new RuntimeException("Only one synthesis query at a time is supported.");
        this.evidenceBlock = evidenceBlock;

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

    // Check if the given block contains statements that are not evidence API calls
    static boolean isLegalEvidenceBlock(Block evidBlock) {
        for (Object obj : evidBlock.statements()) {
            try {
                Statement stmt = (Statement) obj;
                Expression expr = ((ExpressionStatement) stmt).getExpression();
                MethodInvocation invocation = (MethodInvocation) expr;

                IMethodBinding binding = invocation.resolveMethodBinding();
                if (binding == null)
                    throw new RuntimeException("Could not resolve binding. " +
                            "Either CLASSPATH is not set correctly, or there is an invalid evidence type.");

                ITypeBinding cls = binding.getDeclaringClass();
                if (cls == null || !cls.getQualifiedName().equals("edu.rice.cs.caper.bayou.annotations.Evidence"))
                    return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        return true;
    }
}

