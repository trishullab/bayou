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
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DOMClassInstanceCreation extends DOMExpression implements Handler {

    ClassInstanceCreation creation;

    @Expose
    String node = "DOMClassInstanceCreation";

    @Expose
    DOMType _type;

    @Expose
    List<DOMExpression> _arguments;

    public DOMClassInstanceCreation() {
        this.node = "DOMClassInstanceCreation";
    }

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

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMClassInstanceCreation))
            return false;
        DOMClassInstanceCreation d = (DOMClassInstanceCreation) o;
        return _type.equals(d._type) && _arguments.equals(d._arguments);
    }

    @Override
    public int hashCode() {
        return 7* _type.hashCode() + 17* _arguments.hashCode();
    }

    private String __toCall() {
        return _type.type + ".new";
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.add(__toCall());
        calls.addAll(_type.bagOfAPICalls());
        for (DOMExpression a : _arguments)
            calls.addAll(a.bagOfAPICalls());
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (Sequence sequence : soFar) {
            sequence.addCall(__toCall());
            if (sequence.getCalls().size() > max_length)
                throw new TooLongSequenceException();
        }
        _type.updateSequences(soFar, max, max_length);
        for (DOMExpression a : _arguments)
            a.updateSequences(soFar, max, max_length);
    }

    @Override
    public int numStatements() {
        return 0;
    }

    @Override
    public int numLoops() {
        return 0;
    }

    @Override
    public int numBranches() {
        return 0;
    }

    @Override
    public int numExcepts() {
        return 0;
    }
}
