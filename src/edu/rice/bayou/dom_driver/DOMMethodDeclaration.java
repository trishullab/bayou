package edu.rice.bayou.dom_driver;

import edu.rice.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class DOMMethodDeclaration implements Handler {

    final MethodDeclaration method;

    public DOMMethodDeclaration(MethodDeclaration method) {
        this.method = method;
    }

    @Override
    public DSubTree handle() {
        return new DOMBlock(method.getBody()).handle();
    }
}
