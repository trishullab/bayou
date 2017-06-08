package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.ReturnStatement;

public class DOMReturnStatement implements Handler {

    final ReturnStatement statement;

    public DOMReturnStatement(ReturnStatement statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(statement.getExpression()).handle();
    }
}
