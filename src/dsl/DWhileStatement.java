package dsl;

import org.eclipse.jdt.core.dom.WhileStatement;

public class DWhileStatement extends DStatement {

    final String node = "DWhileStatement";
    final DExpression cond;
    final DStatement body;

    private DWhileStatement(DExpression cond, DStatement body) {
        this.cond = cond;
        this.body = body;
    }

    public static class Handle extends Handler {
        WhileStatement statement;

        public Handle(WhileStatement statement, Visitor visitor) {
            super(visitor);
            this.statement = statement;
        }

        @Override
        public DStatement handle() {
            DExpression cond = new DExpression.Handle(statement.getExpression(), visitor).handle();
            DStatement body = new DStatement.Handle(statement.getBody(), visitor).handle();

            if (cond != null)
                return new DWhileStatement(cond, body);
            if (body != null)
                return body;

            return null;
        }
    }
}
