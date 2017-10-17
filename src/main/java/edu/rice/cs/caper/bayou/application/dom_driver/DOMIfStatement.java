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
import edu.rice.cs.caper.bayou.core.dsl.DBranch;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.IfStatement;

public class DOMIfStatement extends DOMStatement implements Handler {

    final IfStatement statement;

    @Expose
    final String node = "DOMIfStatement";

    @Expose
    final DOMExpression _cond;

    @Expose
    final DOMStatement _then;

    @Expose
    final DOMStatement _else;

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
}
