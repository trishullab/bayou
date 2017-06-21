package edu.rice.bayou.dom_driver;

import edu.rice.bayou.dsl.DLoop;
import edu.rice.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ForStatement;

public class DOMForStatement implements Handler {

    final ForStatement statement;

    public DOMForStatement(ForStatement statement) {
        this.statement = statement;
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
}
