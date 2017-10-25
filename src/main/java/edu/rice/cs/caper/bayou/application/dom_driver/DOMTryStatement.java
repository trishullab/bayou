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
import edu.rice.cs.caper.bayou.core.dsl.DExcept;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DOMTryStatement extends DOMStatement implements Handler {

    TryStatement statement;

    @Expose
    String node = "DOMTryStatement";

    @Expose
    DOMBlock _body;

    @Expose
    List<DOMCatchClause> _clauses;

    @Expose
    DOMBlock _finally;

    public DOMTryStatement() {
        this.node = "DOMTryStatement";
    }

    public DOMTryStatement(TryStatement statement) {
        this.statement = statement;
        this._body = new DOMBlock(statement.getBody()).handleAML();
        this._clauses = new ArrayList<>();
        for (Object o : statement.catchClauses())
            _clauses.add(new DOMCatchClause((CatchClause) o).handleAML());
        this._finally = statement.getFinally() != null?
                new DOMBlock(statement.getFinally()).handleAML() : null;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        // restriction: considering only the first catch clause
        DSubTree Ttry = new DOMBlock(statement.getBody()).handle();
        DSubTree Tcatch;
        if (! statement.catchClauses().isEmpty())
            Tcatch = new DOMCatchClause((CatchClause) statement.catchClauses().get(0)).handle();
        else
            Tcatch = new DSubTree();
        DSubTree Tfinally = new DOMBlock(statement.getFinally()).handle();

        boolean except = Ttry.isValid() && Tcatch.isValid();

        if (except)
            tree.addNode(new DExcept(Ttry.getNodes(), Tcatch.getNodes()));
        else {
            // only one of these will add nodes
            tree.addNodes(Ttry.getNodes());
            tree.addNodes(Tcatch.getNodes());
        }

        tree.addNodes(Tfinally.getNodes());

        return tree;
    }

    @Override
    public DOMTryStatement handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMTryStatement))
            return false;
        DOMTryStatement d = (DOMTryStatement) o;
        return _body.equals(d._body) && _clauses.equals(d._clauses);
    }

    @Override
    public int hashCode() {
        return 7* _body.hashCode() + 17* _clauses.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.addAll(_body.bagOfAPICalls());
        for (DOMCatchClause c : _clauses)
            calls.addAll(c.bagOfAPICalls());
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        _body.updateSequences(soFar, max, max_length);
        List<List<Sequence>> copies = new ArrayList<>();
        for (DOMCatchClause c : _clauses) {
            List<Sequence> copy = new ArrayList<>();
            for (Sequence seq : soFar)
                copy.add(new Sequence(seq.getCalls()));
            c.updateSequences(copy, max, max_length);
            copies.add(copy);
        }
        for (List<Sequence> copy : copies)
            for (Sequence seq : copy)
                if (!soFar.contains(seq))
                    soFar.add(seq);
    }

    @Override
    public int numStatements() {
        return _body.numStatements() + _clauses.stream().mapToInt(c -> c.numStatements()).sum() + 1;
    }

    @Override
    public int numLoops() {
        return _body.numLoops() + _clauses.stream().mapToInt(c -> c.numLoops()).sum();
    }

    @Override
    public int numBranches() {
        return _body.numBranches() + _clauses.stream().mapToInt(c -> c.numBranches()).sum();
    }

    @Override
    public int numExcepts() {
        return _body.numExcepts() + _clauses.stream().mapToInt(c -> c.numExcepts()).sum() + 1;
    }

    @Override
    public String toAML() {
        List<String> clauses = _clauses.stream().map(c -> c.toAML()).collect(Collectors.toList());
        return String.format("try %s %s", _body.toAML(), String.join("", clauses));
    }
}
