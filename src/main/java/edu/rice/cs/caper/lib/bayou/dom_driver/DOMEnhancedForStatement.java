package edu.rice.cs.caper.lib.bayou.dom_driver;


import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.EnhancedForStatement;

public class DOMEnhancedForStatement implements Handler {

    final EnhancedForStatement statement;

    public DOMEnhancedForStatement(EnhancedForStatement statement) {
        this.statement = statement;
    }

    /* TODO: handle this properly, by creating a call to Iterator.hasNext() and next() in a loop */
    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        DSubTree Texpr = new DOMExpression(statement.getExpression()).handle();
        DSubTree Tbody = new DOMStatement(statement.getBody()).handle();

        tree.addNodes(Texpr.getNodes());
        tree.addNodes(Tbody.getNodes());

        return tree;
    }
}
