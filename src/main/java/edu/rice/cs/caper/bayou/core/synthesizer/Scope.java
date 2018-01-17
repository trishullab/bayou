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

import java.util.*;

/**
 * A scope of variables for the synthesizer to work with
 */
public class Scope {
    /**
     * The set of variables in the scope
     */
    private Set<Variable> variables;

    /**
     * The set of phantom variables in the scope. Phantom variables are those
     * that cannot be referenced during synthesis because they don't appear in
     * the current scope. But they need to be added to the method's variable
     * declarations because they're used in some inner scope.
     */
    private Set<Variable> phantomVariables;

    /**
     * Initializes the scope
     *
     * @param variables variables present in the scope
     */
    public Scope(List<Variable> variables) {
        this.variables = new HashSet<>(variables);
        this.phantomVariables = new HashSet<>();
    }

    /**
     * Initializes the scope from another scope
     *
     * @param scope scope whose variables are used for initialization
     */
    public Scope(Scope scope) {
        this.variables = new HashSet<>(scope.variables);
        this.phantomVariables = new HashSet<>(scope.phantomVariables);
    }

    /**
     * Gets the set of variables in the current scope
     *
     * @return set of variables
     */
    public Set<Variable> getVariables() {
        return variables;
    }

    /**
     * Gets the set of phantom variables in the current scope
     *
     * @return set of phantom variables
     */
    public Set<Variable> getPhantomVariables() {
        return phantomVariables;
    }

    /**
     * Adds the given variable, whose properties must already be set, to the current scope
     *
     * @param var the variable to be added
     */
    public void addVariable(Variable var) {
        String uniqueName = makeUnique(var.getName());
        var.refactor(uniqueName);
        variables.add(var);
    }

    /**
     * Adds a variable with the given type and properties to the current scope
     *
     * @param type       variable type, from which a variable name will be derived
     * @param properties variable properties
     * @return a TypedExpression with a simple name (variable name) and variable type
     */
    public Variable addVariable(Type type, VariableProperties properties) {
        // construct a nice name for the variable
        String name = createNameFromType(type);

        // add variable to scope and return it
        String uniqueName = makeUnique(name);
        Variable var = new Variable(uniqueName, type, properties);
        variables.add(var);
        return var;
    }

    /**
     * Creates a pretty name from a type
     *
     * @param type type from which name is created
     * @return the pretty name
     */
    private String createNameFromType(Type type) {
        if (type.C().isPrimitive())
            return type.C().getCanonicalName().substring(0, 1);
        String name = type.C().getCanonicalName();
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray())
            if (Character.isUpperCase(c))
                sb.append(c);
        return sb.toString().toLowerCase();
    }

    /**
     * Make a given name unique in the current scope by appending an incrementing id to it
     *
     * @param name name that has to be made unique
     * @return the unique name
     */
    private String makeUnique(String name) {
        Set<String> existingNames = new HashSet<>();
        for (Variable var : variables)
            existingNames.add(var.getName());
        for (Variable var : phantomVariables)
            existingNames.add(var.getName());

        if (!existingNames.contains(name))
            return name;

        int i;
        for (i = 1; i < 9999; i++)
            if (!existingNames.contains(name + i))
                return name + i;
        return null;
    }

    /**
     * Join a list of sub-scopes into this scope.
     * In the join operation, variables declared in ALL sub-scopes will be added to this scope.
     * Variables declared only in some sub-scopes will be added as phantom variables to this scope.
     * Variables that have their "join" flag set to false (e.g., catch clause vars) will be discarded.
     * Finally, variables will be refactored if necessary.
     *
     * @param subScopes list of sub-scopes that have to be joined into this scope
     */
    public void join(List<Scope> subScopes) {
        Set<Variable> common = new HashSet<>(subScopes.get(0).getVariables());
        for (Scope subScope : subScopes)
            common.retainAll(subScope.getVariables());
        common.removeAll(variables);
        for (Variable var : common)
            if (var.isJoinVar())
                variables.add(var);

        Set<String> varNames = new HashSet<>();
        Set<Variable> toRefactor = new HashSet<>();

        for (Scope subScope : subScopes) {
            Set<Variable> uncommon = subScope.getVariables();
            uncommon.removeAll(variables);
            uncommon.removeAll(common);
            for (Variable var : uncommon) {
                if (!var.isJoinVar())
                    continue;

                // Check if another scope added a variable with the same name, and refactor if so.
                // Note: if the other variable also had the same type (which is fine), it would've
                // been added to "common" above, and then to the current variables in scope.
                if (varNames.contains(var.getName()))
                    toRefactor.add(var);

                varNames.add(var.getName());
                phantomVariables.add(var);
            }

            for (Variable var : subScope.getPhantomVariables()) {
                if (phantomVariables.contains(var))
                    continue;
                if (varNames.contains(var.getName()))
                    toRefactor.add(var);
                varNames.add(var.getName());
                phantomVariables.add(var);
            }
        }

        for (Variable var : toRefactor)
            var.refactor(makeUnique(var.getName()));
    }
}
