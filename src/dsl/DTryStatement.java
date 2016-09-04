package dsl;

import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;

import java.util.ArrayList;
import java.util.List;

public class DTryStatement extends DStatement {

    final String node = "DTryStatement";
    final DBlock tryBlock;
    final List<DCatchClause> catchClauses;

    private DTryStatement(DBlock tryBlock, List<DCatchClause> catchClauses) {
        this.tryBlock = tryBlock;
        this.catchClauses = catchClauses;
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

            if (tryBlock != null)
                return new DTryStatement(tryBlock, catchClauses);

            return null;
        }
    }
}
