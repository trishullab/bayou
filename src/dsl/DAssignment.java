package dsl;

import org.eclipse.jdt.core.dom.Assignment;

public class DAssignment extends DExpression {

    final String node = "DAssignment";
    final DExpression lhs;
    final DExpression rhs;
    final DAssignment.Operator operator;

    enum Operator {
        ASSIGN,
    }

    public DAssignment(DExpression lhs, DAssignment.Operator operator, DExpression rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
        this.operator = operator;
    }

    public static class Handle extends Handler {
        Assignment assignment;

        public Handle(Assignment assignment, Visitor visitor) {
            super(visitor);
            this.assignment = assignment;
        }

        @Override
        public DAssignment handle() {
            DAssignment.Operator op = null;

            if (assignment.getOperator().equals(Assignment.Operator.ASSIGN))
                op = DAssignment.Operator.ASSIGN;

            DExpression dlhs = new DExpression.Handle(assignment.getLeftHandSide(), visitor).handle();
            DExpression drhs = new DExpression.Handle(assignment.getRightHandSide(), visitor).handle();

            if (dlhs != null && drhs != null && op != null)
                if (dlhs instanceof DName && drhs instanceof DName)
                    return null;
                else if (dlhs instanceof DName && drhs instanceof DNullLiteral)
                    return null;
                else
                    return new DAssignment(dlhs, op, drhs);

            return null;
        }
    }
}
