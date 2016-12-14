package dsl;

import org.eclipse.jdt.core.dom.IfStatement;

public class DIfStatement extends DStatement {

    final String node = "DIfStatement";
    final DExpression cond;
    final DStatement thenStmt;
    final DStatement elseStmt;

    public DIfStatement(DExpression cond, DStatement thenStmt, DStatement elseStmt) {
        this.cond = cond;
        this.thenStmt = thenStmt;
        this.elseStmt = elseStmt;
    }

    public static class Handle extends Handler {
        IfStatement statement;

        public Handle(IfStatement statement, Visitor visitor) {
            super(visitor);
            this.statement = statement;
        }

        @Override
        public DStatement handle() {
            DExpression cond = new DExpression.Handle(statement.getExpression(), visitor).handle();
            DStatement thenStmt = new DStatement.Handle(statement.getThenStatement(), visitor).handle();
            DStatement elseStmt = new DStatement.Handle(statement.getElseStatement(), visitor).handle();

            if (thenStmt != null && elseStmt != null)
                return new DIfStatement(cond, thenStmt, elseStmt);
            if (thenStmt != null)
                return thenStmt;
            if (elseStmt != null)
                return elseStmt;
            return null;
        }
    }
}
