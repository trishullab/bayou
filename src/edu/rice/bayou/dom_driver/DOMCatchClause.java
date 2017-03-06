package edu.rice.bayou.dom_driver;

import edu.rice.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.CatchClause;

public class DOMCatchClause implements Handler {

    final CatchClause clause;

    public DOMCatchClause(CatchClause clause) {
        this.clause = clause;
    }

    @Override
    public DSubTree handle() {
        return new DOMBlock(clause.getBody()).handle();
    }
}
