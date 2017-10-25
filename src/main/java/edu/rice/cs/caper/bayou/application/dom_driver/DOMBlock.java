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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.Statement;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DOMBlock extends DOMStatement implements Handler {

    Block block;

    @Expose
    String node = "DOMBlock";

    @Expose
    List<DOMStatement> _statements;

    public DOMBlock() {
        this.node = "DOMBlock";
    }

    public DOMBlock(Block block) {
        this.block = block;
        this._statements = new ArrayList<>();
        if (block != null)
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

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof DOMBlock))
            return false;
        DOMBlock d = (DOMBlock) o;
        if (_statements.size() != d._statements.size())
            return false;
        for (int i = 0; i < _statements.size(); i++)
            if (! _statements.get(i).equals(d._statements.get(i)))
                return false;
        return true;
    }

    @Override
    public int hashCode() {
        return _statements.hashCode();
    }

    @Override
    public Set<String> bagOfAPICalls() {
        Set<String> calls = new HashSet<>();
        for (DOMStatement s : _statements)
            calls.addAll(s.bagOfAPICalls());
        return calls;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length)
            throws TooManySequencesException, TooLongSequenceException {
        for (DOMStatement s : _statements)
            s.updateSequences(soFar, max, max_length);
    }

    @Override
    public int numStatements() {
        return _statements.stream().mapToInt(s -> s.numStatements()).sum();
    }

    @Override
    public int numLoops() {
        return _statements.stream().mapToInt(s -> s.numLoops()).sum();
    }

    @Override
    public int numBranches() {
        return _statements.stream().mapToInt(s -> s.numBranches()).sum();
    }

    @Override
    public int numExcepts() {
        return _statements.stream().mapToInt(s -> s.numExcepts()).sum();
    }
}
