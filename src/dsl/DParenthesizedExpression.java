package dsl;

import org.eclipse.jdt.core.dom.ParenthesizedExpression;

public class DParenthesizedExpression extends DExpression {

    final String node = "DParenthesizedExpression";
    final DExpression expression;

    private DParenthesizedExpression(DExpression expression) {
        this.expression = expression;
    }

    public static class Handle extends Handler {

        ParenthesizedExpression expression;

        public Handle(ParenthesizedExpression expression, Visitor visitor) {
            super(visitor);
            this.expression = expression;
        }

        @Override
        public DParenthesizedExpression handle() {
            DExpression exp = new DExpression.Handle(expression.getExpression(), visitor).handle();
            if (exp != null)
                return new DParenthesizedExpression(exp);
            return null;
        }
    }
}
