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
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import java.util.ArrayList;
import java.util.List;

public class DOMVariableDeclarationExpression extends DOMExpression implements Handler {

    final VariableDeclarationExpression expression;

    @Expose
    final String node = "DOMVariableDeclarationExpression";

    @Expose
    final DOMType _type;

    @Expose
    final List<DOMVariableDeclarationFragment> _fragments;

    public DOMVariableDeclarationExpression(VariableDeclarationExpression expression) {
        this.expression = expression;
        this._type = new DOMType(expression.getType()).handleAML();
        this._fragments = new ArrayList<>();
        for (Object o : expression.fragments())
            _fragments.add(new DOMVariableDeclarationFragment((VariableDeclarationFragment) o).handleAML());
    }

    @Override
    public DSubTree handle() {
        DSubTree tree = new DSubTree();
        for (Object o : expression.fragments()) {
            VariableDeclarationFragment fragment = (VariableDeclarationFragment) o;
            DSubTree t = new DOMVariableDeclarationFragment(fragment).handle();
            tree.addNodes(t.getNodes());
        }

        return tree;
    }

    @Override
    public DOMVariableDeclarationExpression handleAML() {
        return this;
    }
}
