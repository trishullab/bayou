package dsl;

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

    public static class Handle extends Handler {
        VariableDeclarationStatement statement;

        public Handle(VariableDeclarationStatement statement, Visitor visitor) {
            super(visitor);
            this.statement = statement;
        }

        @Override
        public DVariableDeclarationStatement handle() {
            List<DVariableDeclarationFragment> fragments = new ArrayList<>();

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
