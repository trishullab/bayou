package dsl;

import org.eclipse.jdt.core.dom.NullLiteral;

import java.util.List;

public class DNullLiteral extends DExpression {

    final String node = "DNullLiteral";

    @Override
    public void updateSequences(List<Sequence> soFar) { }

    @Override
    public String sketch() {
        return "null";
    }

    public static class Handle extends Handler {

        public Handle(NullLiteral __ununsed__, Visitor visitor) {
            super(visitor);
        }

        @Override
        public DNullLiteral handle() {
            return new DNullLiteral();
        }
    }
}
