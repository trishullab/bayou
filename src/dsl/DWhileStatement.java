package dsl;

import org.eclipse.jdt.core.dom.WhileStatement;

import java.util.List;

public class DWhileStatement extends DStatement {

    final String node = "DWhileStatement";
    final DExpression cond;
    final DStatement body;

    // needed for updateSequences, but should not be in JSON
    private transient Visitor visitor;

    private DWhileStatement(DExpression cond, DStatement body) {
        this.cond = cond;
        this.body = body;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (int i = 0; i < visitor.options.NUM_UNROLLS; i++) {
            cond.updateSequences(soFar);
            body.updateSequences(soFar);
        }
        cond.updateSequences(soFar);
    }

    public DWhileStatement setVisitor(Visitor visitor) {
        this.visitor = visitor;
        return this;
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

            if (cond != null && body != null)
                return new DWhileStatement(cond, body).setVisitor(visitor);
            if (body != null)
                return body;

            return null;
        }
    }
}
