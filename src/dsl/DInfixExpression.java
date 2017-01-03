package dsl;

import org.eclipse.jdt.core.dom.InfixExpression;

import java.util.List;

public class DInfixExpression extends DExpression {

    final String node = "DInfixExpression";
    final DExpression left;
    final DExpression right;
    final DInfixExpression.Operator operator;

    public enum Operator {
        EQUALS,
        NOT_EQUALS,
    }

    public DInfixExpression(DExpression left, DInfixExpression.Operator operator, DExpression right) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        left.updateSequences(soFar);
        right.updateSequences(soFar);
    }

    public static class Handle extends Handler {
        InfixExpression expr;

        public Handle(InfixExpression expr, Visitor visitor) {
            super(visitor);
            this.expr = expr;
        }

        @Override
        public DInfixExpression handle() {
            DInfixExpression.Operator op = null;

            if (expr.getOperator().equals(InfixExpression.Operator.EQUALS))
                op = DInfixExpression.Operator.EQUALS;
            else if (expr.getOperator().equals(InfixExpression.Operator.NOT_EQUALS))
                op = DInfixExpression.Operator.NOT_EQUALS;

            DExpression dleft = new DExpression.Handle(expr.getLeftOperand(), visitor).handle();
            DExpression dright = new DExpression.Handle(expr.getRightOperand(), visitor).handle();

            if (dleft != null && dright != null && op != null)
                return new DInfixExpression(dleft, op, dright);

            return null;
        }
    }
}
