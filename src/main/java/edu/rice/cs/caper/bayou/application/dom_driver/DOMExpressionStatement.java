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
import org.eclipse.jdt.core.dom.ExpressionStatement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMExpressionStatement extends DOMStatement implements Handler {

    ExpressionStatement statement;

    @Expose
    String node = "DOMExpressionStatement";

    @Expose
    DOMExpression _expression;

    public DOMExpressionStatement() {
        this.node = "DOMExpressionStatement";
    }

    public DOMExpressionStatement(ExpressionStatement statement) {
        this.statement = statement;
        this._expression = new DOMExpression(statement.getExpression()).handleAML();
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(statement.getExpression()).handle();
    }

    @Override
    public DOMExpressionStatement handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMExpressionStatement))
            return false;
        DOMExpressionStatement d = (DOMExpressionStatement) o;
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
        return 1;
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
