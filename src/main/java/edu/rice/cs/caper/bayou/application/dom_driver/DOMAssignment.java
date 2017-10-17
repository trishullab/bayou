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
import org.eclipse.jdt.core.dom.Assignment;

public class DOMAssignment extends DOMExpression implements Handler {

    final Assignment assignment;

    @Expose
    final String node = "DOMAssignment";

    @Expose
    final DOMExpression _lhs;

    @Expose
    final DOMExpression _rhs;

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
}
