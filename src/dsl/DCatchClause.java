package dsl;

import org.eclipse.jdt.core.dom.CatchClause;

import java.util.List;

public class DCatchClause extends DASTNode {

    final String node = "DCatchClause";
    final DBlock body;

    private DCatchClause(DBlock body) {
        this.body = body;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        body.updateSequences(soFar);
    }

    public static class Handle extends Handler {
        CatchClause catchClause;

        public Handle(CatchClause catchClause, Visitor visitor) {
            super(visitor);
            this.catchClause = catchClause;
        }

        @Override
        public DCatchClause handle() {
            DBlock body = new DBlock.Handle(catchClause.getBody(), visitor).handle();
            if (body != null)
                return new DCatchClause(body);

            return null;
        }
    }
}
