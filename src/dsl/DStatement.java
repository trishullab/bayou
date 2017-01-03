package dsl;

import org.eclipse.jdt.core.dom.*;

public abstract class DStatement extends DASTNode {

    public static class Handle extends Handler {
        Statement statement;

        public Handle(Statement statement, Visitor visitor) {
            super(visitor);
            this.statement = statement;
        }

        @Override
        public DStatement handle() {
            if (statement instanceof Block)
                return new DBlock.Handle((Block) statement, visitor).handle();
            if (statement instanceof ExpressionStatement)
                return new DExpressionStatement.Handle((ExpressionStatement) statement, visitor).handle();
            if (statement instanceof IfStatement)
                return new DIfStatement.Handle((IfStatement) statement, visitor).handle();
            if (statement instanceof WhileStatement)
                return new DWhileStatement.Handle((WhileStatement) statement, visitor).handle();
            if (statement instanceof TryStatement)
                return new DTryStatement.Handle((TryStatement) statement, visitor).handle();
            if (statement instanceof VariableDeclarationStatement)
                return new DVariableDeclarationStatement.Handle((VariableDeclarationStatement) statement, visitor).handle();

            return null;
        }
    }
}
