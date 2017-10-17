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
import org.eclipse.jdt.core.dom.PrefixExpression;

public class DOMPrefixExpression extends DOMExpression implements Handler {

    final PrefixExpression expression;

    @Expose
    final String node = "DOMPrefixExpression";

    @Expose
    final DOMExpression _expression;

    @Expose
    final String _operator;

    public DOMPrefixExpression(PrefixExpression expression) {
        this.expression = expression;
        this._expression = new DOMExpression(expression.getOperand()).handleAML();
        this._operator = expression.getOperator().toString();
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(expression.getOperand()).handle();
    }

    @Override
    public DOMPrefixExpression handleAML() {
        return this;
    }
}
