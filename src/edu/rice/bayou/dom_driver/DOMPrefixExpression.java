package edu.rice.bayou.dom_driver;

import edu.rice.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.PrefixExpression;

public class DOMPrefixExpression implements Handler {

    final PrefixExpression expression;

    public DOMPrefixExpression(PrefixExpression expression) {
        this.expression = expression;
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(expression.getOperand()).handle();
    }
}
