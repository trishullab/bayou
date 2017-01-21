package dom_driver;

import dsl.*;
import org.eclipse.jdt.core.dom.IfStatement;

public class DOMIfStatement implements Handler {

    final IfStatement statement;

    public DOMIfStatement(IfStatement statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        DSubTree Tcond = new DOMExpression(statement.getExpression()).handle();
        DSubTree Tthen = new DOMStatement(statement.getThenStatement()).handle();
        DSubTree Telse = new DOMStatement(statement.getElseStatement()).handle();

        boolean branch = (Tcond.isValid() && Tthen.isValid()) || (Tcond.isValid() && Telse.isValid())
                || (Tthen.isValid() && Telse.isValid());

        if (branch)
            tree.addNode(new DBranch(Tcond.getNodesAsCalls(), Tthen.getNodes(), Telse.getNodes()));
        else {
            // only one of these will add nodes, the rest will add nothing
            tree.addNodes(Tcond.getNodes());
            tree.addNodes(Tthen.getNodes());
            tree.addNodes(Telse.getNodes());
        }
        return tree;
    }
}
