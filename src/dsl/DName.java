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

    @Override
    public String sketch() {
        return "<type:" + type + ">";
    }

    @Override
    public void updateSequences(List<Sequence> soFar) { }

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
            if (type != null && type.getQualifiedName() != null) {
                String className = type.getQualifiedName();
                if (className.contains("<")) /* be agnostic to generic versions */
                    className = className.substring(0, className.indexOf("<"));
                /* consider only java or android types, ignoring those local to the package */
                if (className.startsWith("android.") || className.startsWith("java.")
                        || className.startsWith("javax."))
                    return new DName(className);
            }

            return null;
        }
    }
}
