package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.SynchronizedStatement;

public class DOMSynchronizedStatement implements Handler {

    final SynchronizedStatement statement;

    public DOMSynchronizedStatement(SynchronizedStatement statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        DSubTree Texpr = new DOMExpression(statement.getExpression()).handle();
        DSubTree Tbody = new DOMBlock(statement.getBody()).handle();

        tree.addNodes(Texpr.getNodes());
        tree.addNodes(Tbody.getNodes());

        return tree;
    }
}
