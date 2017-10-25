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
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMCatchClause extends DOMNode implements Handler {

    final CatchClause clause;

    @Expose
    final String node = "DOMCatchClause";

    @Expose
    final DOMType _type;

    @Expose
    final String _variable; // terminal

    @Expose
    final DOMBlock _body;

    public DOMCatchClause(CatchClause clause) {
        this.clause = clause;
        this._type = new DOMType(clause.getException().getType()).handleAML();
        this._variable = clause.getException().getName().getIdentifier();
        this._body = new DOMBlock(clause.getBody()).handleAML();
    }

    @Override
    public DSubTree handle() {
        return new DOMBlock(clause.getBody()).handle();
    }

    @Override
    public DOMCatchClause handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMCatchClause))
            return false;
        DOMCatchClause d = (DOMCatchClause) o;
        return _type.equals(d._type) && _body.equals(d._body);
    }

    @Override
    public int hashCode() {
        return 7* _type.hashCode() + 17* _body.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.addAll(_type.bagOfAPICalls());
        calls.addAll(_body.bagOfAPICalls());
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
        _type.updateSequences(soFar, max, max_length);
        _body.updateSequences(soFar, max, max_length);
    }

    @Override
    public int numStatements() {
        return _body.numStatements();
    }

    @Override
    public int numLoops() {
        return _body.numLoops();
    }

    @Override
    public int numBranches() {
        return _body.numBranches();
    }

    @Override
    public int numExcepts() {
        return _body.numExcepts();
    }

    @Override
    public String toAML() {
        return String.format("catch (%s %s) %s", _type.toAML(), _variable, _body.toAML());
    }
}
