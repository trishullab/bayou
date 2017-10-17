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
import edu.rice.cs.caper.bayou.core.dsl.DExcept;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;

import java.util.ArrayList;
import java.util.List;

public class DOMTryStatement extends DOMStatement implements Handler {

    final TryStatement statement;

    @Expose
    final String node = "DOMTryStatement";

    @Expose
    final DOMBlock _body;

    @Expose
    final List<DOMCatchClause> _clauses;

    @Expose
    final DOMBlock _finally;

    public DOMTryStatement(TryStatement statement) {
        this.statement = statement;
        this._body = new DOMBlock(statement.getBody()).handleAML();
        this._clauses = new ArrayList<>();
        for (Object o : statement.catchClauses())
            _clauses.add(new DOMCatchClause((CatchClause) o).handleAML());
        this._finally = statement.getFinally() != null?
                new DOMBlock(statement.getFinally()).handleAML() : null;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        // restriction: considering only the first catch clause
        DSubTree Ttry = new DOMBlock(statement.getBody()).handle();
        DSubTree Tcatch;
        if (! statement.catchClauses().isEmpty())
            Tcatch = new DOMCatchClause((CatchClause) statement.catchClauses().get(0)).handle();
        else
            Tcatch = new DSubTree();
        DSubTree Tfinally = new DOMBlock(statement.getFinally()).handle();

        boolean except = Ttry.isValid() && Tcatch.isValid();

        if (except)
            tree.addNode(new DExcept(Ttry.getNodes(), Tcatch.getNodes()));
        else {
            // only one of these will add nodes
            tree.addNodes(Ttry.getNodes());
            tree.addNodes(Tcatch.getNodes());
        }

        tree.addNodes(Tfinally.getNodes());

        return tree;
    }

    @Override
    public DOMTryStatement handleAML() {
        return this;
    }
}
