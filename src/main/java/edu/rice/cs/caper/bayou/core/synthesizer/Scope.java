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

public class Scope {
    private Set<Variable> variables;

    /* Phantom variables are those that cannot be referenced because they
     * don't appear in the current scope. But they need to be added to the
     * method's variable declarations because they're used in some inner scope.
     */
    private Set<Variable> phantomVariables;

    public Scope(List<Variable> variables) {
        this.variables = new HashSet<>(variables);
        this.phantomVariables = new HashSet<>();
    }

    public Scope(Scope scope) {
        this.variables = new HashSet<>(scope.variables);
        this.phantomVariables = new HashSet<>(scope.phantomVariables);
    }

    public Set<Variable> getVariables() {
        return variables;
    }

    public Set<Variable> getPhantomVariables() {
        return phantomVariables;
    }

    public Variable addVariable(SearchTarget target) {
        Type type = target.getType();

        // construct a nice name for the variable
        String name = createNameFromType(type);

        // add variable to scope and return it
        String uniqueName = makeUnique(name);
        Variable var = new Variable(uniqueName, type);
        variables.add(var);
        return var;
    }

    private String createNameFromType(Type type) {
        String name = type.C().getSimpleName();
        name = name.replaceAll("\\[\\]", "s");
        name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        return name;
    }

    private String makeUnique(String name) {
        List<String> existingNames = new ArrayList<>();
        for (Variable var : variables)
            existingNames.add(var.getName());
        for (Variable var : phantomVariables)
            existingNames.add(var.getName());

        int i;
        for (i = 1; i < 9999; i++)
            if (!existingNames.contains(name + i))
                return name + i;
        return null;
    }

    /* Given a list of sub-scopes, join all of them into this scope.
     * In the join operation, variables declared in ALL sub-scopes will be added to this scope.
     * Variables declared only in some sub-scopes will be added as phantom variables to this scope.
     * Variables that have their "join" flag set to false (e.g., catch clause vars) will be discarded.
     */
    public void join(List<Scope> subScopes) {
        Set<Variable> common = new HashSet<>(subScopes.get(0).getVariables());
        for (Scope subScope : subScopes)
            common.retainAll(subScope.getVariables());
        common.removeAll(variables);
        for (Variable var : common)
            if (var.isJoinVar())
                variables.add(var);

        for (Scope subScope : subScopes) {
            Set<Variable> uncommon = subScope.getVariables();
            uncommon.removeAll(variables);
            uncommon.removeAll(common);
            for (Variable var : uncommon)
                if (var.isJoinVar())
                    phantomVariables.add(var);
            phantomVariables.addAll(subScope.getPhantomVariables());
        }
    }
}
