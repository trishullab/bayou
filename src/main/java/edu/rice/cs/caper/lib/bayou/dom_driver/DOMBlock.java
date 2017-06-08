package edu.rice.cs.caper.lib.bayou.dom_driver;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

public class DOMBlock implements Handler {

    final Block block;

    public DOMBlock(Block block) {
        this.block = block;
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        if (block != null)
            for (Object o : block.statements()) {
                DOMStatement statement = new DOMStatement((Statement) o);
                DSubTree t = statement.handle();
                tree.addNodes(t.getNodes());
            }
        return tree;
    }
}
