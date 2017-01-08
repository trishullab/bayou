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

    @Override
    public String sketch() {
        String s = "try " + (tryBlock == null? "{" + HOLE() + "}": tryBlock.sketch());
        for (DCatchClause clause : catchClauses)
            s += clause == null? HOLE() : clause.sketch();
        return s;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        tryBlock.updateSequences(soFar);
        for (DCatchClause clause : catchClauses) {
            List<Sequence> copy = new ArrayList<>();
            for (Sequence seq : soFar)
                copy.add(new Sequence(seq.calls));
            clause.updateSequences(copy);
            for (Sequence seq : copy)
                if (!soFar.contains(seq))
                    soFar.add(seq);
        }
    }

    public static class Handle extends Handler {
        TryStatement statement;

        public Handle(TryStatement statement, Visitor visitor) {
            super(visitor);
            this.statement = statement;
        }

        @Override
        public DStatement handle() {
            DBlock tryBlock = new DBlock.Handle(statement.getBody(), visitor).handle();
            List<DCatchClause> catchClauses = new ArrayList<>();
            for (Object o : statement.catchClauses()) {
                CatchClause clause = (CatchClause) o;
                DCatchClause dclause = new DCatchClause.Handle(clause, visitor).handle();
                if (dclause != null)
                    catchClauses.add(dclause);
            }

            if (tryBlock != null && catchClauses.size() > 0)
                return new DTryStatement(tryBlock, catchClauses);
            if (tryBlock != null)
                return tryBlock;

            return null;
        }
    }
}
