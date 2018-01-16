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

/**
 * A variable in the synthesizer's type system
 */
public class Variable {

    /**
     * Name of the variable
     */
    private final String name;

    /**
     * Type of the variable
     */
    private final Type type;

    /**
     * Reference count of the variable (used for cost metric)
     */
    private int refCount;

    /**
     * Denotes if the variable is a user-defined variable in the param/body of the method
     */
    private boolean userVar;

    /**
     * Denotes if the variable should participate in joins (e.g., catch clause variables will not)
     */
    private boolean join;

    /**
     * Denotes if the variable needs to be initialized to a default value using $init
     */
    private boolean defaultInit;

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
        this.join = properties.getJoin();
        this.userVar = properties.getUserVar();
        this.defaultInit = properties.getDefaultInit();
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
        return join;
    }

    /**
     * Checks if this variable is a user-defined variable
     * @return current value
     */
    public boolean isUserVar() {
        return userVar;
    }

    /**
     * Checks if a default initializer needs to be synthesized for this variable
     * @return current value
     */
    public boolean isDefaultInit() {
        return defaultInit;
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

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof Variable))
            return false;
        Variable v = (Variable) o;
        return v.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name + ":" + type;
    }
}
