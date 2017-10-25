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
import org.eclipse.jdt.core.dom.ParenthesizedExpression;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMParenthesizedExpression extends DOMExpression implements Handler {

    ParenthesizedExpression expression;

    @Expose
    String node = "DOMParenthesizedExpression";

    @Expose
    DOMExpression _expression;

    public DOMParenthesizedExpression() {
        this.node = "DOMParenthesizedExpression";
    }

    public DOMParenthesizedExpression(ParenthesizedExpression expression) {
        this.expression = expression;
        this._expression = new DOMExpression(expression.getExpression()).handleAML();
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(expression.getExpression()).handle();
    }

    @Override
    public DOMParenthesizedExpression handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMParenthesizedExpression))
            return false;
        DOMParenthesizedExpression d = (DOMParenthesizedExpression) o;
        return _expression.equals(d._expression);
    }

    @Override
    public int hashCode() {
        return _expression.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.addAll(_expression.bagOfAPICalls());
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
        _expression.updateSequences(soFar, max, max_length);
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

    @Override
    public String toAML() {
        return "(" + _expression.toAML() + ")";
    }
}
