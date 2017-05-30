package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DExcept;
import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
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
