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

import edu.rice.cs.caper.bayou.core.dsl.DAPICall;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.Stack;

public class DOMMethodInvocation implements Handler {

    final MethodInvocation invocation;

    public DOMMethodInvocation(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        // add the expression's subtree (e.g: foo(..).bar() should handle foo(..) first)
        DSubTree Texp = new DOMExpression(invocation.getExpression()).handle();
        tree.addNodes(Texp.getNodes());

        // evaluate arguments first
        for (Object o : invocation.arguments()) {
            DSubTree Targ = new DOMExpression((Expression) o).handle();
            tree.addNodes(Targ.getNodes());
        }

        IMethodBinding binding = invocation.resolveMethodBinding();
        // get to the generic declaration, if this binding is an instantiation
        while (binding != null && binding.getMethodDeclaration() != binding)
            binding = binding.getMethodDeclaration();
        MethodDeclaration localMethod = Utils.checkAndGetLocalMethod(binding);
        if (localMethod != null) {
            Stack<MethodDeclaration> callStack = Visitor.V().callStack;
            if (! callStack.contains(localMethod)) {
                callStack.push(localMethod);
                DSubTree Tmethod = new DOMMethodDeclaration(localMethod).handle();
                callStack.pop();
                tree.addNodes(Tmethod.getNodes());
            }
        }
        else if (Utils.isRelevantCall(binding))
            tree.addNode(new DAPICall(binding, Visitor.V().getLineNumber(invocation)));
        return tree;
    }
}

