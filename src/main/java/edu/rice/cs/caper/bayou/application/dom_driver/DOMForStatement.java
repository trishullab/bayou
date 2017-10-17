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
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;

import java.util.ArrayList;
import java.util.List;

public class DOMForStatement extends DOMStatement implements Handler {

    final ForStatement statement;

    @Expose
    final String node = "DOMForStatement";

    @Expose
    final List<DOMExpression> _init;

    @Expose
    final DOMExpression _cond;

    @Expose
    final List<DOMExpression> _update;

    @Expose
    final DOMStatement _body;

    public DOMForStatement(ForStatement statement) {
        this.statement = statement;
        this._init = new ArrayList<>();
        for (Object o : statement.initializers())
            _init.add(new DOMExpression((Expression) o).handleAML());
        this._cond = statement.getExpression() != null?
                new DOMExpression(statement.getExpression()).handleAML() : null;
        this._update = new ArrayList<>();
        for (Object o : statement.updaters())
            _update.add(new DOMExpression((Expression) o).handleAML());
        this._body = new DOMStatement(statement.getBody()).handleAML();
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        for (Object o : statement.initializers()) {
            DSubTree init = new DOMExpression((Expression) o).handle();
            tree.addNodes(init.getNodes());
        }
        DSubTree cond = new DOMExpression(statement.getExpression()).handle();
        DSubTree body = new DOMStatement(statement.getBody()).handle();
        for (Object o : statement.updaters()) {
            DSubTree update = new DOMExpression((Expression) o).handle();
            body.addNodes(update.getNodes()); // updaters are part of body
        }

        boolean loop = cond.isValid();

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
    public DOMForStatement handleAML() {
        return this;
    }
}
