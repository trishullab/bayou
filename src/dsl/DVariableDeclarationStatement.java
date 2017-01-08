package dsl;

import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.ArrayList;
import java.util.List;

public class DVariableDeclarationStatement extends DStatement {

    final String node = "DVariableDeclarationStatement";
    List<DVariableDeclarationFragment> fragments;

    private DVariableDeclarationStatement(List<DVariableDeclarationFragment> fragments) {
        this.fragments = fragments;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DVariableDeclarationFragment fragment : fragments)
            fragment.updateSequences(soFar);
    }

    @Override
    public String sketch() {
        String s = "";
        int i = 0;
        for (DVariableDeclarationFragment fragment : fragments) {
            s += (fragment == null? HOLE() : fragment.sketch());
            if (i < fragments.size()-1)
                s += ",";
        }
        s += ";";
        return s;
    }

    public static class Handle extends Handler {
        VariableDeclarationStatement statement;

        public Handle(VariableDeclarationStatement statement, Visitor visitor) {
            super(visitor);
            this.statement = statement;
        }

        @Override
        public DVariableDeclarationStatement handle() {
            Type t = statement.getType();
            if (! (t.isSimpleType() || t.isParameterizedType()))
                return null;

            List<DVariableDeclarationFragment> fragments = new ArrayList<>();
            ITypeBinding binding = t.resolveBinding();

            if (binding == null)
                return null;

            String className = binding.getQualifiedName();
            if (className.contains("<")) /* be agnostic to generic versions */
                className = className.substring(0, className.indexOf("<"));
            if (! visitor.options.API_CLASSES.contains(className))
                return null;

            for (Object o : statement.fragments()) {
                VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
                DVariableDeclarationFragment dfragment = new DVariableDeclarationFragment.Handle(fragment, visitor).handle();
                if (dfragment != null)
                    fragments.add(dfragment);
            }

            if (! fragments.isEmpty())
                return new DVariableDeclarationStatement(fragments);

            return null;
        }
    }
}
