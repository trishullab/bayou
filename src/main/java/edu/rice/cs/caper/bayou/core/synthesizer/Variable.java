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

import org.eclipse.jdt.core.dom.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A variable in the synthesizer's type system
 */
public class Variable {

    /**
     * Name of the variable
     */
    private String name;

    /**
     * Type of the variable
     */
    private final Type type;

    /**
     * Reference count of the variable (used for cost metric)
     */
    private int refCount;

    /**
     * Set of AST node references of this variable. If/when the variable is refactored, these
     * AST nodes will be updated. Nodes are automatically added when createASTNode() is called.
     */
    private Set<SimpleName> astNodeRefs;

    /**
     * Properties of this variable
     */
    private VariableProperties properties;

    /**
     * Initializes a variable with the given parameters
     * @param name variable name
     * @param type variable type
     * @param properties variable properties
     */
    Variable(String name, Type type, VariableProperties properties) {
        this.name = name;
        this.type = type;
        this.refCount = 0;
        this.astNodeRefs = new HashSet<>();
        this.properties = properties;
    }

    /**
     * Gets the variable name
     * @return variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the variable type
     * @return variable type
     */
    public Type getType() {
        return type;
    }

    /**
     * Checks if this variable can participate in joins
     * @return current value
     */
    public boolean isJoinVar() {
        return properties.getJoin();
    }

    /**
     * Checks if this variable is a user-defined variable
     * @return current value
     */
    public boolean isUserVar() {
        return properties.getUserVar();
    }

    /**
     * Checks if a default initializer needs to be synthesized for this variable
     * @return current value
     */
    public boolean isDefaultInit() {
        return properties.getDefaultInit();
    }

    /**
     * Increments the reference counter of this variable
     */
    public void addRefCount() {
        refCount += 1;
    }

    /**
     * Gets the reference counter of this variable
     * @return current value
     */
    public int getRefCount() {
        return refCount;
    }

    /**
     * Creates and associates an AST node (of type SimpleName) referring to this variable
     * @param ast the owner of the node
     * @return the AST node corresponding to this variable
     */
    public SimpleName createASTNode(AST ast) {
        SimpleName node = ast.newSimpleName(getName());
        astNodeRefs.add(node);
        return node;
    }

    /**
     * Refactors this variable's name and updates all AST nodes associated with this variable.
     * It is the responsibility of the refactoring method to ensure the name is unique wherever
     * this variable is referenced. Note: a variable's type cannot be refactored.
     * @param newName the new name of this variable
     */
    public void refactor(String newName) {
        this.name = newName;
        for (SimpleName node : astNodeRefs)
            node.setIdentifier(newName);
    }

    /**
     * Creates a default initializer expression for this variable
     * @param ast the owner of the expression
     * @return expression that initializes this variable
     */
    public Expression createDefaultInitializer(AST ast) {
        CastExpression cast = ast.newCastExpression();
        cast.setType(type.simpleT(ast));

        MethodInvocation invocation = ast.newMethodInvocation();
        invocation.setExpression(ast.newSimpleName("Bayou"));
        invocation.setName(ast.newSimpleName("$init"));
        cast.setExpression(invocation);

        return cast;
    }

    /**
     * Compares two variables based on their name AND type
     * @param o the object to compare with
     * @return whether they are equal
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof Variable))
            return false;
        Variable v = (Variable) o;
        return v.getName().equals(getName()) && v.getType().equals(getType());
    }

    @Override
    public int hashCode() {
        return 7*name.hashCode() + 17*type.hashCode();
    }

    /**
     * Returns a string representation of this variable (for debug purposes only)
     * @return string
     */
    @Override
    public String toString() {
        return name + ":" + type;
    }
}
