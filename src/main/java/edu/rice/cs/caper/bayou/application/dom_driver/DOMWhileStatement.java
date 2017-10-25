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
import edu.rice.cs.caper.bayou.core.dsl.DLoop;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMWhileStatement extends DOMStatement implements Handler {

    final WhileStatement statement;

    @Expose
    final String node = "DOMWhileStatement";

    @Expose
    final DOMExpression _cond;

    @Expose
    final DOMStatement _body;

    public DOMWhileStatement(WhileStatement statement) {
        this.statement = statement;
        this._cond = new DOMExpression(statement.getExpression()).handleAML();
        this._body = new DOMStatement(statement.getBody()).handleAML();
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        DSubTree cond = new DOMExpression(statement.getExpression()).handle();
        DSubTree body = new DOMStatement(statement.getBody()).handle();

        boolean loop = cond.isValid();

        if (loop)
            tree.addNode(new DLoop(cond.getNodesAsCalls(), body.getNodes()));
        else {
            // only one of these will add nodes
            tree.addNodes(cond.getNodes());
            tree.addNodes(body.getNodes());
        }

        return tree;
    }

    @Override
    public DOMWhileStatement handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMWhileStatement))
            return false;
        DOMWhileStatement d = (DOMWhileStatement) o;
        return _cond.equals(d._cond) && _body.equals(d._body);
    }

    @Override
    public int hashCode() {
        return 7* _cond.hashCode() + 17* _body.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.addAll(_cond.bagOfAPICalls());
        calls.addAll(_body.bagOfAPICalls());
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        _cond.updateSequences(soFar, max, max_length);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.getCalls()));
        _body.updateSequences(copy, max, max_length);
        _cond.updateSequences(copy, max, max_length);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
    }

    @Override
    public int numStatements() {
        return _body.numStatements() + 1;
    }

    @Override
    public int numLoops() {
        return _body.numLoops() + 1;
    }

    @Override
    public int numBranches() {
        return _body.numBranches();
    }

    @Override
    public int numExcepts() {
        return _body.numExcepts();
    }
}
