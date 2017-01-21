package dom_driver;

import dsl.DAPICall;
import dsl.DSubTree;
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
        IMethodBinding binding = invocation.resolveMethodBinding();
        MethodDeclaration localConstructor = Utils.checkAndGetLocalMethod(binding);
        if (localConstructor != null) {
            DSubTree Tconstructor = new DOMMethodDeclaration(localConstructor).handle();
            tree.addNodes(Tconstructor.getNodes());
        }
        else if (Utils.isRelevantCall(binding))
            tree.addNode(new DAPICall(binding));
        return tree;
    }
}

