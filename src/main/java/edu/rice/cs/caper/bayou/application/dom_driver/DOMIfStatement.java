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
import edu.rice.cs.caper.bayou.core.dsl.DBranch;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.eclipse.jdt.core.dom.IfStatement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMIfStatement extends DOMStatement implements Handler {

    IfStatement statement;

    @Expose
    String node = "DOMIfStatement";

    @Expose
    DOMExpression _cond;

    @Expose
    DOMStatement _then;

    @Expose
    DOMStatement _else;

    public DOMIfStatement() {
        this.node = "DOMIfStatement";
    }

    public DOMIfStatement(IfStatement statement) {
        this.statement = statement;
        this._cond = new DOMExpression(statement.getExpression()).handleAML();
        this._then = new DOMStatement(statement.getThenStatement()).handleAML();
        this._else = statement.getElseStatement() != null?
                new DOMStatement(statement.getElseStatement()).handleAML() : null;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        DSubTree Tcond = new DOMExpression(statement.getExpression()).handle();
        DSubTree Tthen = new DOMStatement(statement.getThenStatement()).handle();
        DSubTree Telse = new DOMStatement(statement.getElseStatement()).handle();

        boolean branch = (Tcond.isValid() && Tthen.isValid()) || (Tcond.isValid() && Telse.isValid())
                || (Tthen.isValid() && Telse.isValid());

        if (branch)
            tree.addNode(new DBranch(Tcond.getNodesAsCalls(), Tthen.getNodes(), Telse.getNodes()));
        else {
            // only one of these will add nodes, the rest will add nothing
            tree.addNodes(Tcond.getNodes());
            tree.addNodes(Tthen.getNodes());
            tree.addNodes(Telse.getNodes());
        }
        return tree;
    }

    @Override
    public DOMIfStatement handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMIfStatement))
            return false;
        DOMIfStatement d = (DOMIfStatement) o;
        return _cond.equals(d._cond) && _then.equals(d._then) && _else.equals(d._else);
    }

    @Override
    public int hashCode() {
        return 7* _cond.hashCode() + 17* _then.hashCode() + 31* _else.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.addAll(_cond.bagOfAPICalls());
        calls.addAll(_then.bagOfAPICalls());
        calls.addAll(_else.bagOfAPICalls());
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
        _then.updateSequences(soFar, max, max_length);
        _else.updateSequences(copy, max, max_length);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
    }

    @Override
    public int numStatements() {
        return _then.numStatements() + _else.numStatements() + 1;
    }

    @Override
    public int numLoops() {
        return _then.numLoops() + _else.numLoops();
    }

    @Override
    public int numBranches() {
        return _then.numBranches() + _else.numBranches() + 1;
    }

    @Override
    public int numExcepts() {
        return _then.numExcepts() + _else.numExcepts();
    }

    @Override
    public String toAML() {
        return String.format("if (%s) %s %s", _cond.toAML(), _then.toAML(), _else.toAML());
    }
}
