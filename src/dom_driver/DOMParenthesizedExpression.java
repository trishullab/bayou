package dom_driver;

import dsl.DSubTree;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;

public class DOMParenthesizedExpression implements Handler {

    final ParenthesizedExpression expression;

    public DOMParenthesizedExpression(ParenthesizedExpression expression) {
        this.expression = expression;
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(expression.getExpression()).handle();
    }
}
