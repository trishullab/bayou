package dom_driver;

import dsl.DBranch;
import dsl.DSubTree;
import org.eclipse.jdt.core.dom.ConditionalExpression;

public class DOMConditionalExpression implements Handler {

    final ConditionalExpression expression;

    public DOMConditionalExpression(ConditionalExpression expression) {
        this.expression = expression;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();

        DSubTree Tcond = new DOMExpression(expression.getExpression()).handle();
        DSubTree Tthen = new DOMExpression(expression.getThenExpression()).handle();
        DSubTree Telse = new DOMExpression(expression.getElseExpression()).handle();

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
