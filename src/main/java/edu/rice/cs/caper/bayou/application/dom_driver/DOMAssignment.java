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
import org.eclipse.jdt.core.dom.Assignment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMAssignment extends DOMExpression implements Handler {

    Assignment assignment;

    @Expose
    String node = "DOMAssignment";

    @Expose
    DOMExpression _lhs;

    @Expose
    DOMExpression _rhs;

    public DOMAssignment() {
        this.node = "DOMAssignment";
    }

    public DOMAssignment(Assignment assignment) {
        this.assignment = assignment;
        this._lhs = new DOMExpression(assignment.getLeftHandSide()).handleAML();
        this._rhs = new DOMExpression(assignment.getRightHandSide()).handleAML();
        // operator not needed in AML because it will always be =
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(assignment.getRightHandSide()).handle();
    }

    @Override
    public DOMAssignment handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMAssignment))
            return false;
        DOMAssignment d = (DOMAssignment) o;
        return _lhs.equals(d._lhs) && _rhs.equals(d._rhs);
    }

    @Override
    public int hashCode() {
        return 7* _lhs.hashCode() + 17* _rhs.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.addAll(_lhs.bagOfAPICalls());
        calls.addAll(_rhs.bagOfAPICalls());
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
        _lhs.updateSequences(soFar, max, max_length);
        _rhs.updateSequences(soFar, max, max_length);
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
