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
package edu.rice.cs.caper.bayou.core.dom_driver;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.*;

public class DOMStatement implements Handler {

    final Statement statement;

    public DOMStatement(Statement statement) {
        this.statement = statement;
    }

    @Override
    public DSubTree handle() {
        if (statement instanceof Block)
            return new DOMBlock((Block) statement).handle();
        if (statement instanceof ExpressionStatement)
            return new DOMExpressionStatement((ExpressionStatement) statement).handle();
        if (statement instanceof IfStatement)
            return new DOMIfStatement((IfStatement) statement).handle();
        if (statement instanceof SwitchStatement)
            return new DOMSwitchStatement((SwitchStatement) statement).handle();
        if (statement instanceof SwitchCase)
            return new DOMSwitchCase((SwitchCase) statement).handle();
        if (statement instanceof DoStatement)
            return new DOMDoStatement((DoStatement) statement).handle();
        if (statement instanceof ForStatement)
            return new DOMForStatement((ForStatement) statement).handle();
        if (statement instanceof EnhancedForStatement)
            return new DOMEnhancedForStatement((EnhancedForStatement) statement).handle();
        if (statement instanceof WhileStatement)
            return new DOMWhileStatement((WhileStatement) statement).handle();
        if (statement instanceof TryStatement)
            return new DOMTryStatement((TryStatement) statement).handle();
        if (statement instanceof VariableDeclarationStatement)
            return new DOMVariableDeclarationStatement((VariableDeclarationStatement) statement).handle();
        if (statement instanceof SynchronizedStatement)
            return new DOMSynchronizedStatement((SynchronizedStatement) statement).handle();
        if (statement instanceof ReturnStatement)
            return new DOMReturnStatement((ReturnStatement) statement).handle();
        if (statement instanceof LabeledStatement)
            return new DOMLabeledStatement((LabeledStatement) statement).handle();

        return new DSubTree();
    }
}
