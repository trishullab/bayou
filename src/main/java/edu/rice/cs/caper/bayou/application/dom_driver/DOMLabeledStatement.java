package edu.rice.bayou.dom_driver;

import edu.rice.bayou.dsl.DSubTree;
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
