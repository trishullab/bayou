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


import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.*;

public class DOMExpression extends DOMNode implements Handler {

    final Expression expression;

    public DOMExpression() { // only used by AML for other DOM classes to inherit this
        this.expression = null;
    }

    public DOMExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public DSubTree handle() {
        if (expression instanceof MethodInvocation)
            return new DOMMethodInvocation((MethodInvocation) expression).handle();
        if (expression instanceof ClassInstanceCreation)
            return new DOMClassInstanceCreation((ClassInstanceCreation) expression).handle();
        if (expression instanceof InfixExpression)
            return new DOMInfixExpression((InfixExpression) expression).handle();
        if (expression instanceof PrefixExpression)
            return new DOMPrefixExpression((PrefixExpression) expression).handle();
        if (expression instanceof ConditionalExpression)
            return new DOMConditionalExpression((ConditionalExpression) expression).handle();
        if (expression instanceof VariableDeclarationExpression)
            return new DOMVariableDeclarationExpression((VariableDeclarationExpression) expression).handle();
        if (expression instanceof Assignment)
            return new DOMAssignment((Assignment) expression).handle();
        if (expression instanceof ParenthesizedExpression)
            return new DOMParenthesizedExpression((ParenthesizedExpression) expression).handle();

        return new DSubTree();
    }

    @Override
    public DOMExpression handleAML() {
        if (expression == null)
            return null;
        if (expression instanceof MethodInvocation)
            return new DOMMethodInvocation((MethodInvocation) expression);
        if (expression instanceof ClassInstanceCreation)
            return new DOMClassInstanceCreation((ClassInstanceCreation) expression);
        if (expression instanceof InfixExpression)
            return new DOMInfixExpression((InfixExpression) expression);
        if (expression instanceof PrefixExpression)
            return new DOMPrefixExpression((PrefixExpression) expression);
        if (expression instanceof ConditionalExpression)
            return new DOMConditionalExpression((ConditionalExpression) expression);
        if (expression instanceof VariableDeclarationExpression)
            return new DOMVariableDeclarationExpression((VariableDeclarationExpression) expression);
        if (expression instanceof Assignment)
            return new DOMAssignment((Assignment) expression);
        if (expression instanceof ParenthesizedExpression)
            return new DOMParenthesizedExpression((ParenthesizedExpression) expression);
        if (expression instanceof NullLiteral)
            return new DOMNullLiteral();
        if (expression instanceof Name)
            return new DOMName((Name) expression);
        if (expression instanceof NumberLiteral)
            return new DOMNumberLiteral((NumberLiteral) expression);

        throw new IllegalArgumentException("Unsupported expression type: " + expression);
    }
}
