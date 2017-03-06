package edu.rice.bayou.dom_driver;

import edu.rice.bayou.dsl.DSubTree;
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
