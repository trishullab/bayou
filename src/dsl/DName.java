package dsl;

import org.eclipse.jdt.core.dom.Name;

public class DName extends DExpression {

    final String node = "DName";

    public static class Handle extends Handler {

        public Handle(Name __unused__, Visitor visitor) {
            super(visitor);
        }

        @Override
        public DName handle() {
            return new DName();
        }
    }
}
