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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

class ExpressionChain {
    static final int MAX_COMPOSE_LENGTH = 3; // a().b().c().d()...
    static final int MAX_ARGUMENT_DEPTH = 2; // a(b(c(d(...))))
    static final int K = 3; // number of arguments is given K times more weight than length of composition in cost

    final Variable var;

    // these two lists are synchronized
    final List<Method> methods;
    final List<Type> types;

    ExpressionChain(Variable var) {
        this.var = var;
        methods = new ArrayList<>();
        types = new ArrayList<>();
    }

    ExpressionChain(ExpressionChain chain) {
        var = chain.var;
        methods = new ArrayList<>(chain.methods);
        types = new ArrayList<>(chain.types);
    }

    void addMethod(Method m) {
        types.add(getCurrentType().getConcretization(m.getGenericReturnType()));
        methods.add(m);
    }

    void pop() {
        methods.remove(methods.size() - 1);
        types.remove(types.size() - 1);
    }

    Type getCurrentType() {
        return types.isEmpty()? var.getType() : types.get(types.size() - 1);
    }

    int structCost() {
        int args = methods.stream().mapToInt(m -> m.getParameterTypes().length).sum();
        int chainLength = methods.size();

        return chainLength + K*args; // give some more weight to arguments because that involves further search
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof ExpressionChain))
            return false;
        return methods.equals(((ExpressionChain) o).methods);
    }

    @Override
    public int hashCode() {
        return methods.hashCode();
    }
}
