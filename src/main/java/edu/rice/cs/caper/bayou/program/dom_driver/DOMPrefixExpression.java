package edu.rice.cs.caper.bayou.program.dom_driver;


import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
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
