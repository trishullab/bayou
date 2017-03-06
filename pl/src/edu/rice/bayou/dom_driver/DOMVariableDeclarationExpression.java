package edu.rice.bayou.dom_driver;

import edu.rice.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class DOMVariableDeclarationExpression implements Handler {

    final VariableDeclarationExpression expression;

    public DOMVariableDeclarationExpression(VariableDeclarationExpression expression) {
        this.expression = expression;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        for (Object o : expression.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
            DSubTree t = new DOMVariableDeclarationFragment(fragment).handle();
            tree.addNodes(t.getNodes());
        }

        return tree;
    }
}
