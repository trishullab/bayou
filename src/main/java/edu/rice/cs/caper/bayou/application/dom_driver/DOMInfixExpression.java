package edu.rice.cs.caper.bayou.application.dom_driver;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.InfixExpression;

public class DOMInfixExpression implements Handler {

    final InfixExpression expr;

    public DOMInfixExpression(InfixExpression expr) {
        this.expr = expr;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        DSubTree Tleft = new DOMExpression(expr.getLeftOperand()).handle();
        DSubTree Tright = new DOMExpression(expr.getRightOperand()).handle();

        tree.addNodes(Tleft.getNodes());
        tree.addNodes(Tright.getNodes());

        return tree;
    }
}
