package dom_driver;

import dsl.DSubTree;
import org.eclipse.jdt.core.dom.ExpressionStatement;

public class DOMExpressionStatement implements Handler {

    final ExpressionStatement statement;

    public DOMExpressionStatement(ExpressionStatement statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(statement.getExpression()).handle();
    }
}
