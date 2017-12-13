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

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.*;
import java.util.*;

public class DCEOptimizor extends ASTVisitor {
    // The def variables
    protected Map<String,List<ASTNode>> defs;

    // The use variables
    protected Map<String,List<ASTNode>> uses;

    // The eliminated variable declarations
    protected Set<String> eliminatedVars;

    public DCEOptimizor() {
        this.defs = new HashMap<>();
        this.uses = new HashMap<>();
        this.eliminatedVars = new HashSet<>();
    }

    // Apply the optimization here
    public Block apply(Block body, DSubTree dAST) {
        // Collect defs and uses
        collectDefUse(body);

        // Check if def has potential uses
        ArrayList<String> tempVars = new ArrayList<>();
        for (String def : defs.keySet()) {
            List<ASTNode> defVals = defs.get(def);
            if (defVals.size() == 1) {
                if (uses.get(def) == null) {
                    // No use, then remove this def's corresponding ExpressionStatement from synthesized code block
                    tempVars.add(def);
                }
            }
        }
        // Clean up the infeasible elimination
        for (String def : tempVars) {
            List<ASTNode> defVals = defs.get(def);
            ASTNode defNode = defVals.get(0);

            if (defNode.getParent() instanceof ExpressionStatement) {
                defNode.getParent().delete();
                this.eliminatedVars.add(def);
            } else if (hasLegalParent(defNode)) {
                this.eliminatedVars.add(def);
            }
        }
        // Apply post optimizations to dAST
        dAST.cleanupCatchClauses(this.eliminatedVars);

        return body;
    }

    protected boolean hasLegalParent(ASTNode node) {
        node = node.getParent();
        while (node != null && (node instanceof ClassInstanceCreation
                || node instanceof ParenthesizedExpression
                || node instanceof Assignment)) {
            node = node.getParent();
        }

        return node != null && node instanceof ExpressionStatement;
    }

    // Collect the def and use variables
    protected void collectDefUse(Block body) {
        body.accept(this);
    }

    public Set<String> getEliminatedVars() {
        return this.eliminatedVars;
    }

    @Override
    public boolean visit(Assignment assign) {
        return true;
    }

    @Override
    public boolean visit(SimpleName name) {
        Statement stmt = getParentStatement(name);
        String varName = name.toString();

        boolean isDef = false;
        ASTNode parent = name.getParent();
        if (parent instanceof Assignment) {
            isDef = ((Assignment)parent).getLeftHandSide() == name
                    && ((Assignment)parent).getRightHandSide() instanceof ClassInstanceCreation
                    && parent.getParent() != null;
        }
        boolean isArgAssignment = false;
        while (parent != null) {
            if (parent instanceof MethodInvocation || parent instanceof ClassInstanceCreation) {
                isArgAssignment = true;
                break;
            }
            parent = parent.getParent();
        }

        if (varName != null && stmt != null) {
            if (isDef && !isArgAssignment)
                // Add variable def
                addToMap(varName, name.getParent(), defs);
            else
                // Add potential use
                addToMap(varName, stmt, uses);
        }

        return false;
    }

    @Override
    public boolean visit(QualifiedName name) {
        Statement stmt = getParentStatement(name);
        String varName = name.toString();

        boolean isDef = false;
        ASTNode parent = name.getParent();
        if (parent instanceof Assignment) {
            isDef = ((Assignment)parent).getLeftHandSide() == name;
        }

        if (varName != null && stmt != null) {
            if (isDef)
                // Add variable def
                addToMap(varName, stmt, defs);
            else
                // Add potential use
                addToMap(varName, stmt, uses);
        }

        return false;
    }

    @Override
    public boolean visit(ConstructorInvocation constInvoke) {
        return true;
    }

    // Add variable and its parent to register map
    protected void addToMap(String varName, ASTNode parent, Map<String,List<ASTNode>> varMap) {
        List<ASTNode> values = varMap.get(varName);
        if (values == null) {
            values = new ArrayList<>();
            varMap.put(varName, values);
        }
        values.add(parent);
    }

    // Get the parent statement
    protected Statement getParentStatement(Expression expr) {
        ASTNode node = expr;
        while (!(node.getParent() instanceof Statement)) {
            node = node.getParent();
        }

        return (Statement)node.getParent();
    }
}
