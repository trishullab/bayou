package dsl;

import org.eclipse.jdt.core.dom.ExpressionStatement;

import java.util.List;

public class DExpressionStatement extends DStatement {

    final String node = "DExpressionStatement";
    final DExpression expression;

    private DExpressionStatement(DExpression expression) {
        this.expression = expression;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        expression.updateSequences(soFar);
    }

    public static class Handle extends Handler {
        ExpressionStatement expressionStatement;

        public Handle(ExpressionStatement expressionStatement, Visitor visitor) {
            super(visitor);
            this.expressionStatement = expressionStatement;
        }

        @Override
        public DExpressionStatement handle() {
            DExpression exp = new DExpression.Handle(expressionStatement.getExpression(), visitor).handle();
            if (exp != null)
                return new DExpressionStatement(exp);
            return null;
        }
    }
}
