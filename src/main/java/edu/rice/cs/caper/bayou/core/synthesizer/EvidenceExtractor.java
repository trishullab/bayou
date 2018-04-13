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
import org.eclipse.jdt.core.dom.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EvidenceExtractor extends ASTVisitor {

    class JSONOutputWrapper {
        Set<String> apicalls;
        Set<String> types;
        Set<String> keywords;

        public JSONOutputWrapper() {
            this.apicalls = new HashSet<>();
            this.types = new HashSet<>();
            this.keywords = new HashSet<>();
        }
    }

    private static Map<String, String> primitives;
    static {
        Map<String, String> map = new HashMap<>();
        map.put("byte", "Byte");
        map.put("short", "Short");
        map.put("int", "Integer");
        map.put("long", "Long");
        map.put("float", "Float");
        map.put("double", "Double");
        map.put("boolean", "Boolean");
        map.put("char", "Character");
        primitives = Collections.unmodifiableMap(map);
    }

    JSONOutputWrapper output = new JSONOutputWrapper();
    Block evidenceBlock;

    public String execute(Parser parser) {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        parser.cu.accept(this);
        return gson.toJson(output);
    }

    @Override
    public boolean visit(MethodDeclaration declaration) throws SynthesisException {
        if (evidenceBlock != null)
            return false; // already extracted evidence

        IMethodBinding binding = declaration.resolveBinding();
        if (binding == null)
            throw new SynthesisException(SynthesisException.CouldNotResolveBinding);

        // add formal parameters to types evidence
        output.types.clear();
        Pattern pattern = Pattern.compile("\\w+");
        for (ITypeBinding param : binding.getParameterTypes()) {
            String p = param.getName();
            Matcher matcher = pattern.matcher(p);
            while (matcher.find())
                output.types.add(checkPrimitive(matcher.group()));
        }

        // check if body contains any other statement than variable declarations
        for (Object o : declaration.getBody().statements()) {
            Statement statement = (Statement) o;
            if (!(statement instanceof VariableDeclarationStatement) && !(statement instanceof Block))
                throw new SynthesisException(SynthesisException.IrrelevantCodeInBody);
        }

        return true;
    }

    @Override
    public boolean visit(MethodInvocation invocation) throws SynthesisException {
        IMethodBinding binding = invocation.resolveMethodBinding();
        if (binding == null)
            throw new SynthesisException(SynthesisException.CouldNotResolveBinding);

        ITypeBinding cls = binding.getDeclaringClass();
        if (cls == null || !cls.getQualifiedName().equals("edu.rice.cs.caper.bayou.annotations.Evidence"))
            return false;

        if (! (invocation.getParent().getParent() instanceof Block))
            throw new SynthesisException(SynthesisException.EvidenceNotInBlock);
        Block evidenceBlock = (Block) invocation.getParent().getParent();

        if (!isLegalEvidenceBlock(evidenceBlock))
            throw new SynthesisException(SynthesisException.EvidenceMixedWithCode);

        if (this.evidenceBlock != null && this.evidenceBlock != evidenceBlock)
            throw new SynthesisException(SynthesisException.MoreThanOneHole);
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
                output.types.add(checkPrimitive(a.getLiteralValue()));
            }
        } else if (binding.getName().equals("keywords")) {
            for (Object arg : invocation.arguments()) {
                StringLiteral a = (StringLiteral) arg;
                output.keywords.add(a.getLiteralValue().toLowerCase());
            }
        } else throw new SynthesisException(SynthesisException.InvalidEvidenceType);

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
                    throw new SynthesisException(SynthesisException.CouldNotResolveBinding);

                ITypeBinding cls = binding.getDeclaringClass();
                if (cls == null || !cls.getQualifiedName().equals("edu.rice.cs.caper.bayou.annotations.Evidence"))
                    return false;
            } catch (ClassCastException e) {
                return false;
            }
        }

        return true;
    }

    String checkPrimitive(String type) {
        return primitives.getOrDefault(type, type);
    }
}

