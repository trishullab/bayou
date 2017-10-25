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
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.eclipse.jdt.core.dom.NumberLiteral;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMNumberLiteral extends DOMExpression {

    @Expose
    String node = "DOMNumberLiteral";

    @Expose
    String _value;

    public DOMNumberLiteral() {
        this.node = "DOMNumberLiteral";
    }

    public DOMNumberLiteral(NumberLiteral number) {
        this._value = number.getToken();
    }

    @Override
    public DOMNumberLiteral handleAML() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMNumberLiteral))
            return false;
        return true; // ignore constant values when comparing AML ASTs
    }

    @Override
    public int hashCode() {
        return 1234;
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
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
