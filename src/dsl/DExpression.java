package dsl;

import org.eclipse.jdt.core.dom.*;

public abstract class DExpression extends DASTNode {

    public static class Handle extends Handler {
        Expression expression;

        public Handle(Expression expression, Visitor visitor) {
            super(visitor);
            this.expression = expression;
        }

        @Override
        public DExpression handle() {
            if (expression instanceof Name)
                return new DName.Handle((Name) expression, visitor).handle();
            if (expression instanceof NullLiteral)
                return new DNullLiteral.Handle((NullLiteral) expression, visitor).handle();
            if (expression instanceof MethodInvocation)
                return new DMethodInvocation.Handle((MethodInvocation) expression, visitor).handle();
            if (expression instanceof ClassInstanceCreation)
                return new DClassInstanceCreation.Handle((ClassInstanceCreation) expression, visitor).handle();
            if (expression instanceof InfixExpression)
                return new DInfixExpression.Handle((InfixExpression) expression, visitor).handle();
            if (expression instanceof Assignment)
                return new DAssignment.Handle((Assignment) expression, visitor).handle();
            if (expression instanceof ParenthesizedExpression)
                return new DParenthesizedExpression.Handle((ParenthesizedExpression) expression, visitor).handle();

            return null;
        }
    }
}
