package dsl;

import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.List;

public class DVariableDeclarationFragment extends DVariableDeclaration {

    final String node = "DVariableDeclarationFragment";
    DName name;
    DExpression initializer;

    private DVariableDeclarationFragment(DName name, DExpression initializer) {
        this.name = name;
        this.initializer = initializer;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        initializer.updateSequences(soFar);
    }

    public static class Handle extends Handler {

        VariableDeclarationFragment fragment;

        public Handle(VariableDeclarationFragment fragment, Visitor visitor) {
            super(visitor);
            this.fragment = fragment;
        }

        @Override
        public DVariableDeclarationFragment handle() {
            DName name = new DName.Handle(fragment.getName(), visitor).handle();
            DExpression initializer = new DExpression.Handle(fragment.getInitializer(), visitor).handle();
            if (name != null && initializer != null)
                return new DVariableDeclarationFragment(name, initializer);

            return null;
        }
    }
}
