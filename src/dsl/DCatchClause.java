package dsl;

import org.eclipse.jdt.core.dom.CatchClause;

public class DCatchClause extends DASTNode {

    final String node = "DCatchClause";
    final DBlock body;

    private DCatchClause(DBlock body) {
        this.body = body;
    }

    public static class Handle extends Handler {
        CatchClause catchClause;

        public Handle(CatchClause catchClause, Visitor visitor) {
            super(visitor);
            this.catchClause = catchClause;
        }

        @Override
        public DCatchClause handle() {
            return new DCatchClause(new DBlock.Handle(catchClause.getBody(), visitor).handle());
        }
    }
}
