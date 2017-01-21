package dom_driver;

import dsl.DSubTree;
import org.eclipse.jdt.core.dom.*;

public class DOMStatement implements Handler {

    final Statement statement;

    public DOMStatement(Statement statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {
        if (statement instanceof Block)
            return new DOMBlock((Block) statement).handle();
        if (statement instanceof ExpressionStatement)
            return new DOMExpressionStatement((ExpressionStatement) statement).handle();
        if (statement instanceof IfStatement)
            return new DOMIfStatement((IfStatement) statement).handle();
        if (statement instanceof DoStatement)
            return new DOMDoStatement((DoStatement) statement).handle();
        if (statement instanceof ForStatement)
            return new DOMForStatement((ForStatement) statement).handle();
        if (statement instanceof WhileStatement)
            return new DOMWhileStatement((WhileStatement) statement).handle();
        if (statement instanceof TryStatement)
            return new DOMTryStatement((TryStatement) statement).handle();
        if (statement instanceof VariableDeclarationStatement)
            return new DOMVariableDeclarationStatement((VariableDeclarationStatement) statement).handle();
        if (statement instanceof SynchronizedStatement)
            return new DOMSynchronizedStatement((SynchronizedStatement) statement).handle();
        if (statement instanceof ReturnStatement)
            return new DOMReturnStatement((ReturnStatement) statement).handle();
        if (statement instanceof LabeledStatement)
            return new DOMLabeledStatement((LabeledStatement) statement).handle();

        return new DSubTree();
    }
}
