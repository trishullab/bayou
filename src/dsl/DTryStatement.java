package dsl;

import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;

import java.util.ArrayList;
import java.util.List;

public class DTryStatement extends DStatement {

    final String node = "DTryStatement";
    final DBlock tryBlock;
    final List<DCatchClause> catchClauses;
    final DBlock finallyBlock;

    private DTryStatement(DBlock tryBlock, List<DCatchClause> catchClauses, DBlock finallyBlock) {
        this.tryBlock = tryBlock;
        this.catchClauses = catchClauses;
        this.finallyBlock = finallyBlock;
    }

    public static class Handle extends Handler {
        TryStatement statement;

        public Handle(TryStatement statement, Visitor visitor) {
            super(visitor);
            this.statement = statement;
        }

        @Override
        public DTryStatement handle() {
            DBlock tryBlock = new DBlock.Handle(statement.getBody(), visitor).handle();
            List<DCatchClause> catchClauses = new ArrayList<>();
            for (Object o : statement.catchClauses()) {
                CatchClause clause = (CatchClause) o;
                DCatchClause dclause = new DCatchClause.Handle(clause, visitor).handle();
                if (dclause != null)
                    catchClauses.add(dclause);
            }
            DBlock finallyBlock = null;
            if (statement.getFinally() != null)
                finallyBlock = new DBlock.Handle(statement.getFinally(), visitor).handle();

            if (tryBlock != null)
                return new DTryStatement(tryBlock, catchClauses, finallyBlock);

            return null;
        }
    }
}
