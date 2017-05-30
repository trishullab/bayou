package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class DOMVariableDeclarationStatement implements Handler {

    final VariableDeclarationStatement statement;

    public DOMVariableDeclarationStatement(VariableDeclarationStatement statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        for (Object o : statement.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
            DSubTree t = new DOMVariableDeclarationFragment(fragment).handle();
            tree.addNodes(t.getNodes());
        }

        return tree;
    }
}
