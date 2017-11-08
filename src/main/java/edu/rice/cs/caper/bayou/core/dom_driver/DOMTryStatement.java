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
package edu.rice.cs.caper.bayou.core.dom_driver;

import edu.rice.cs.caper.bayou.core.dsl.DExcept;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;

public class DOMTryStatement implements Handler {

    final TryStatement statement;

    public DOMTryStatement(TryStatement statement) {
        this.statement = statement;
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
}
