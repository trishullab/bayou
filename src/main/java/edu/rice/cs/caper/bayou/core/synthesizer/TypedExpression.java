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
package edu.rice.cs.caper.bayou.core.synthesizer;

import org.eclipse.jdt.core.dom.Expression;

import java.util.HashSet;
import java.util.Set;

/**
 * A wrapper for an expression with a type in the synthesizer
 * Setter methods return the same object for easy chaining
 */
public class TypedExpression {

    /**
     * The DOM expression
     */
    private Expression expression;

    /**
     * The synthesizer type
     */
    private Type type;

    /**
     * The variables that are referred to in the expression
     */
    private Set<Variable> referencedVariables;

    /**
     * The imports that are associated with this expression
     */
    private Set<Class> importsAssociated;

    /**
     * Creates a TypedExpression given a DOM expression and the type
     *
     * @param expression the DOM expression
     * @param type       the expression type
     */
    public TypedExpression(Expression expression, Type type) {
        this.expression = expression;
        this.type = type;
        this.referencedVariables = new HashSet<>();
        this.importsAssociated = new HashSet<>();
    }

    /**
     * Gets the type of this TypedExpression
     *
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the expression of this TypedExpression
     *
     * @return the expression
     */
    public Expression getExpression() {
        return expression;
    }

    /**
     * Gets the variables referred to in the expression
     *
     * @return list of variables
     */
    public Set<Variable> getReferencedVariables() {
        return referencedVariables;
    }

    /**
     * Adds a variable that is referred to in the expression
     *
     * @param var referenced variable
     * @return this object for chaining
     */
    public TypedExpression addReferencedVariable(Variable var) {
        referencedVariables.add(var);
        return this;
    }

    /**
     * Adds all referenced variables in the given set to this expression
     *
     * @param vars the set of referenced variables
     * @return this object for chaining
     */
    public TypedExpression addReferencedVariables(Set<Variable> vars) {
        referencedVariables.addAll(vars);
        return this;
    }

    /**
     * Gets the set of imports associated with this expression
     *
     * @return set of imports
     */
    public Set<Class> getAssociatedImports() {
        return importsAssociated;
    }

    /**
     * Adds an import that is associated with this expression
     *
     * @param cls the imported class
     * @return this object for chaining
     */
    public TypedExpression addAssociatedImport(Class cls) {
        importsAssociated.add(cls);
        return this;
    }

    /**
     * Adds a set of imports that are associated with this expression
     *
     * @param cls the imported classes
     * @return this object for chaining
     */
    public TypedExpression addAssociatedImports(Set<Class> cls) {
        importsAssociated.addAll(cls);
        return this;
    }
}
