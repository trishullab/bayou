package dsl;

import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.Name;

import java.util.List;

public class DName extends DExpression {

    final String node = "DName";
    final String type;

    public DName(String type) {
        this.type = type;
    }

    public static class Handle extends Handler {

        Name name;
        public Handle(Name name, Visitor visitor) {
            super(visitor);
            this.name = name;
        }

        @Override
        public DName handle() {
            IBinding binding = name.resolveBinding();
            if (binding == null || ! (binding instanceof IVariableBinding))
                return null;
            ITypeBinding type = ((IVariableBinding) binding).getType();
            if (type != null && type.getDeclaringClass() != null) {
                String className = type.getDeclaringClass().getQualifiedName();
                if (className.contains("<")) /* be agnostic to generic versions */
                    className = className.substring(0, className.indexOf("<"));
                /* consider only java or android types, ignoring those local to the package */
                if (className.startsWith("android.") || className.startsWith("java.")
                        || className.startsWith("javax."))
                    return new DName(className);
            }

            return null;
        }

        @Override
        public void updateSequences(List<Sequence> soFar) { }
    }
}
