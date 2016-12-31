package dsl;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

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

        @Override
        public void updateSequences(List<Sequence> soFar) {
            if (expression instanceof Name)
                new DName.Handle((Name) expression, visitor).updateSequences(soFar);
            if (expression instanceof NullLiteral)
                new DNullLiteral.Handle((NullLiteral) expression, visitor).updateSequences(soFar);
            if (expression instanceof MethodInvocation)
                new DMethodInvocation.Handle((MethodInvocation) expression, visitor).updateSequences(soFar);
            if (expression instanceof ClassInstanceCreation)
                new DClassInstanceCreation.Handle((ClassInstanceCreation) expression, visitor).updateSequences(soFar);
            if (expression instanceof InfixExpression)
                new DInfixExpression.Handle((InfixExpression) expression, visitor).updateSequences(soFar);
            if (expression instanceof Assignment)
                new DAssignment.Handle((Assignment) expression, visitor).updateSequences(soFar);
            if (expression instanceof ParenthesizedExpression)
                new DParenthesizedExpression.Handle((ParenthesizedExpression) expression, visitor).updateSequences(soFar);
        }
    }
}
