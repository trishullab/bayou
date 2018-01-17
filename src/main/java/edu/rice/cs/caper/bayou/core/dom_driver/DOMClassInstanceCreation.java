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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class DOMClassInstanceCreation implements Handler {

    final ClassInstanceCreation creation;
    final Visitor visitor;

    public DOMClassInstanceCreation(ClassInstanceCreation creation, Visitor visitor) {
        this.creation = creation;
        this.visitor = visitor;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        // add the expression's subtree (e.g: foo(..).bar() should handle foo(..) first)
        DSubTree Texp = new DOMExpression(creation.getExpression(), visitor).handle();
        tree.addNodes(Texp.getNodes());

        // evaluate arguments first
        for (Object o : creation.arguments()) {
            DSubTree Targ = new DOMExpression((Expression) o, visitor).handle();
            tree.addNodes(Targ.getNodes());
        }

        IMethodBinding binding = creation.resolveConstructorBinding();

        /* commenting this in order to concretize generics
         * // get to the generic declaration, if this binding is an instantiation
         * while (binding != null && binding.getMethodDeclaration() != binding)
         *     binding = binding.getMethodDeclaration();
        */

        MethodDeclaration localMethod = Utils.checkAndGetLocalMethod(binding, visitor);
        if (localMethod != null) {
            DSubTree Tmethod = new DOMMethodDeclaration(localMethod, visitor).handle();
            tree.addNodes(Tmethod.getNodes());
        }
        else if (Utils.isRelevantCall(binding, visitor)) {
            try {
                tree.addNode(new DAPICall(binding, visitor.getLineNumber(creation)));
            } catch (DAPICall.InvalidAPICallException e) {
                // continue without adding the node
            }
        }
        return tree;
    }
}
