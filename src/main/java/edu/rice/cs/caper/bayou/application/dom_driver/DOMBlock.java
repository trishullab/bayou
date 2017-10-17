/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.application.dom_driver;

import com.google.gson.annotations.Expose;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.List;

public class DOMBlock extends DOMStatement implements Handler {

    final Block block;

    @Expose
    final String node = "DOMBlock";

    @Expose
    final List<DOMStatement> _statements;

    public DOMBlock(Block block) {
        this.block = block;
        this._statements = new ArrayList<>();
        for (Object o : block.statements())
            this._statements.add(new DOMStatement((Statement) o).handleAML());
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

    @Override
    public DOMBlock handleAML() {
        return this;
    }
}
