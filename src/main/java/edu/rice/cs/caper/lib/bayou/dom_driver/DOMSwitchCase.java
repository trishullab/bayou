package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
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


