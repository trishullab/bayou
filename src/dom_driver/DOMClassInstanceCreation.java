package dom_driver;

import dsl.DAPICall;
import dsl.DSubTree;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;

public class DOMClassInstanceCreation implements Handler {

    final ClassInstanceCreation creation;

    public DOMClassInstanceCreation(ClassInstanceCreation creation) {
        this.creation = creation;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        IMethodBinding binding = creation.resolveConstructorBinding();
        MethodDeclaration localMethod = Utils.checkAndGetLocalMethod(binding);
        if (localMethod != null) {
            DSubTree Tmethod = new DOMMethodDeclaration(localMethod).handle();
            tree.addNodes(Tmethod.getNodes());
        }
        else if (Utils.isRelevantCall(binding))
            tree.addNode(new DAPICall(binding));
        return tree;
    }
}
