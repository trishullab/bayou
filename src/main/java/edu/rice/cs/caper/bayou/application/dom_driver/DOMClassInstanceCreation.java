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
package edu.rice.cs.caper.bayou.application.dom_driver;


import com.google.gson.annotations.Expose;
import edu.rice.cs.caper.bayou.core.dsl.DAPICall;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

public class DOMClassInstanceCreation extends DOMExpression implements Handler {

    final ClassInstanceCreation creation;

    @Expose
    final String node = "DOMClassInstanceCreation";

    @Expose
    final DOMType _type;

    @Expose
    final List<DOMExpression> _arguments;

    public DOMClassInstanceCreation(ClassInstanceCreation creation) {
        this.creation = creation;
        this._type = new DOMType(creation.getType()).handleAML();
        this._arguments = new ArrayList<>();
        for (Object o : creation.arguments())
            this._arguments.add(new DOMExpression((Expression) o).handleAML());
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        // add the expression's subtree (e.g: foo(..).bar() should handle foo(..) first)
        DSubTree Texp = new DOMExpression(creation.getExpression()).handle();
        tree.addNodes(Texp.getNodes());

        // evaluate arguments first
        for (Object o : creation.arguments()) {
            DSubTree Targ = new DOMExpression((Expression) o).handle();
            tree.addNodes(Targ.getNodes());
        }

        IMethodBinding binding = creation.resolveConstructorBinding();
        // get to the generic declaration, if this binding is an instantiation
        while (binding != null && binding.getMethodDeclaration() != binding)
            binding = binding.getMethodDeclaration();

        // program-generation branch: ALL calls are relevant and there are no interprocedural calls
        tree.addNode(new DAPICall(binding, 999));
        return tree;
    }

    @Override
    public DOMClassInstanceCreation handleAML() {
        return this;
    }
}
