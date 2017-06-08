package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.Assignment;

public class DOMAssignment implements Handler {

    final Assignment assignment;

    public DOMAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

    @Override
    public DSubTree handle() {
        return new DOMExpression(assignment.getRightHandSide()).handle();
    }
}
