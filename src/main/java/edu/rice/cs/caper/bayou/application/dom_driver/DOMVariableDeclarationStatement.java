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
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.ArrayList;
import java.util.List;

public class DOMVariableDeclarationStatement extends DOMStatement implements Handler {

    final VariableDeclarationStatement statement;

    @Expose
    final String node = "DOMVariableDeclarationStatement";

    @Expose
    final DOMType _type;

    @Expose
    final List<DOMVariableDeclarationFragment> _fragments;

    public DOMVariableDeclarationStatement(VariableDeclarationStatement statement) {
        this.statement = statement;
        this._type = new DOMType(statement.getType()).handleAML();
        this._fragments = new ArrayList<>();
        for (Object o : statement.fragments())
            _fragments.add(new DOMVariableDeclarationFragment((VariableDeclarationFragment) o).handleAML());
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        for (Object o : statement.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
            DSubTree t = new DOMVariableDeclarationFragment(fragment).handle();
            tree.addNodes(t.getNodes());
        }

        return tree;
    }

    @Override
    public DOMVariableDeclarationStatement handleAML() {
        return this;
    }
}
