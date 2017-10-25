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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMMethodInvocation extends DOMExpression implements Handler {

    final MethodInvocation invocation;

    @Expose
    final String node = "DOMMethodInvocation";

    @Expose
    final DOMExpression _expression;

    @Expose
    final String _name; // terminal

    @Expose
    final List<DOMExpression> _arguments;

    public DOMMethodInvocation(MethodInvocation invocation) {
        this.invocation = invocation;
        this._expression = invocation.getExpression() != null?
                new DOMExpression(invocation.getExpression()).handleAML() : null;
        this._name = invocation.getName().getFullyQualifiedName();
        this._arguments = new ArrayList<>();
        for (Object o : invocation.arguments())
            _arguments.add(new DOMExpression((Expression) o).handleAML());
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

        // program-generation branch: ALL calls are relevant and there are no interprocedural calls
        tree.addNode(new DAPICall(binding, 999));
        return tree;
    }

    @Override
    public DOMMethodInvocation handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMMethodInvocation))
            return false;
        DOMMethodInvocation d = (DOMMethodInvocation) o;
        return _expression.equals(d._expression) && _name.equals(d._name) && _arguments.equals(d._arguments);
    }

    @Override
    public int hashCode() {
        return 7* _expression.hashCode() + 17* _name.hashCode() + 31* _arguments.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.add(_name);
        calls.addAll(_expression.bagOfAPICalls());
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
            sequence.addCall(_name);
            if (sequence.getCalls().size() > max_length)
                throw new TooLongSequenceException();
        }
        _expression.updateSequences(soFar, max, max_length);
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

