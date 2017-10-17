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
import edu.rice.cs.caper.bayou.core.dsl.DLoop;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.DoStatement;

public class DOMDoStatement extends DOMStatement implements Handler {

    final DoStatement statement;

    @Expose
    final String node = "DOMDoStatement";

    @Expose
    final DOMExpression _cond;

    @Expose
    final DOMStatement _body;

    public DOMDoStatement(DoStatement statement) {
        this.statement = statement;
        this._cond = new DOMExpression(statement.getExpression()).handleAML();
        this._body = new DOMStatement(statement.getBody()).handleAML();
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        DSubTree cond = new DOMExpression(statement.getExpression()).handle();
        DSubTree body = new DOMStatement(statement.getBody()).handle();

        boolean loop = cond.isValid() && body.isValid();

        tree.addNodes(body.getNodes());
        if (loop)
            tree.addNode(new DLoop(cond.getNodesAsCalls(), body.getNodes()));
        else {
            // only one of these will add nodes
            tree.addNodes(cond.getNodes());
            tree.addNodes(body.getNodes());
        }

        return tree;
    }

    @Override
    public DOMDoStatement handleAML() {
        return this;
    }
}
