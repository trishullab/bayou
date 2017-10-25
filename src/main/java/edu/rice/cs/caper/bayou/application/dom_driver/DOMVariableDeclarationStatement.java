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
import edu.rice.cs.caper.bayou.core.dsl.Sequence;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMVariableDeclarationStatement))
            return false;
        DOMVariableDeclarationStatement d = (DOMVariableDeclarationStatement) o;
        return _type.equals(d._type) && _fragments.equals(d._fragments);
    }

    @Override
    public int hashCode() {
        return 7* _type.hashCode() + 17* _fragments.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        calls.addAll(_type.bagOfAPICalls());
        for (DOMVariableDeclarationFragment f : _fragments)
            calls.addAll(f.bagOfAPICalls());
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
        _type.updateSequences(soFar, max, max_length);
        for (DOMVariableDeclarationFragment f : _fragments)
            f.updateSequences(soFar, max, max_length);
    }

    @Override
    public int numStatements() {
        return 1;
    }

    @Override
    public int numLoops() {
        return 0;
    }

    @Override
    public int numBranches() {
        return 0;
    }

    @Override
    public int numExcepts() {
        return 0;
    }
}
