package edu.rice.cs.caper.bayou.program.dom_driver;


import edu.rice.cs.caper.bayou.core.dsl.DLoop;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.DoStatement;

public class DOMDoStatement implements Handler {

    final DoStatement statement;

    public DOMDoStatement(DoStatement statement) {
        this.statement = statement;
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
}
