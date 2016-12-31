package dsl;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

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

        @Override
        public void updateSequences(List<Sequence> soFar) {
            if (statement instanceof Block)
                new DBlock.Handle((Block) statement, visitor).updateSequences(soFar);
            if (statement instanceof ExpressionStatement)
                new DExpressionStatement.Handle((ExpressionStatement) statement, visitor).updateSequences(soFar);
            if (statement instanceof IfStatement)
                new DIfStatement.Handle((IfStatement) statement, visitor).updateSequences(soFar);
            if (statement instanceof WhileStatement)
                new DWhileStatement.Handle((WhileStatement) statement, visitor).updateSequences(soFar);
            if (statement instanceof TryStatement)
                new DTryStatement.Handle((TryStatement) statement, visitor).updateSequences(soFar);
            if (statement instanceof VariableDeclarationStatement)
                new DVariableDeclarationStatement.Handle((VariableDeclarationStatement) statement, visitor).updateSequences(soFar);
        }
    }
}
