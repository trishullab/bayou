package dsl;

import org.eclipse.jdt.core.dom.NullLiteral;

public class DNullLiteral extends DExpression {

    final String node = "DNullLiteral";

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
