package edu.rice.cs.caper.bayou.program.dom_driver;

import edu.rice.cs.caper.bayou.core.dsl.DAPICall;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;

public class DOMMethodInvocation implements Handler {

    final MethodInvocation invocation;

    public DOMMethodInvocation(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        // add the expression's subtree (e.g: foo(..).bar() should handle foo(..) first)
        DSubTree Texp = new DOMExpression(invocation.getExpression()).handle();
        tree.addNodes(Texp.getNodes());

        // evaluate arguments first
        for (Object o : invocation.arguments()) {
            DSubTree Targ = new DOMExpression((Expression) o).handle();
            tree.addNodes(Targ.getNodes());
        }

        IMethodBinding binding = invocation.resolveMethodBinding();
        MethodDeclaration localConstructor = Utils.checkAndGetLocalMethod(binding);
        if (localConstructor != null) {
            DSubTree Tconstructor = new DOMMethodDeclaration(localConstructor).handle();
            tree.addNodes(Tconstructor.getNodes());
        }
        else if (Utils.isRelevantCall(binding))
            tree.addNode(new DAPICall(binding, Visitor.V().getLineNumber(invocation)));
        return tree;
    }
}

