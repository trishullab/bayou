package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.LabeledStatement;

public class DOMLabeledStatement implements Handler {

    final LabeledStatement statement;

    public DOMLabeledStatement(LabeledStatement statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {
        return new DOMStatement(statement.getBody()).handle();
    }
}
