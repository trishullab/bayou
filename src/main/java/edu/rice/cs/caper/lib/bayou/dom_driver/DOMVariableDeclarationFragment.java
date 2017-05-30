package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class DOMVariableDeclarationFragment implements Handler {

    final VariableDeclarationFragment fragment;

    public DOMVariableDeclarationFragment(VariableDeclarationFragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(fragment.getInitializer()).handle();
    }
}
