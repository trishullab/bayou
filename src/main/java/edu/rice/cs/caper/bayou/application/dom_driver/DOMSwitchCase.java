package edu.rice.bayou.dom_driver;

import edu.rice.bayou.dsl.*;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.SwitchCase;

public class DOMSwitchCase implements Handler {

    final SwitchCase statement;

    public DOMSwitchCase(SwitchCase statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {


        DSubTree tree = new DSubTree();

        return tree;
    }
}


