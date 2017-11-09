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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class Enumerator {

    final AST ast;
    final Environment env;
    static final int MAX_COMPOSE_LENGTH = 3; // a().b().c().d()...
    static final int MAX_ARGUMENT_DEPTH = 2; // a(b(c(d(...))))
    static final int K = 3; // number of arguments is given K times more weight than length of composition in cost

    class InvocationChain {
        final Variable var;
        final List<Method> methods;
        final List<Type> types;

        InvocationChain(Variable var) {
            this.var = var;
            methods = new ArrayList<>();
            types = new ArrayList<>();
        }

        InvocationChain(InvocationChain chain) {
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
            if (o == null || ! (o instanceof InvocationChain))
                return false;
            return methods.equals(((InvocationChain) o).methods);
        }

        @Override
        public int hashCode() {
            return methods.hashCode();
        }
    }

    private final Set<Class> importsDuringSearch;

    public Enumerator(AST ast, Environment env) {
        this.ast = ast;
        this.env = env;
        this.importsDuringSearch = new HashSet<>();
    }

    public TypedExpression search(Type targetType) {
        TypedExpression tExpr = search(targetType, 0);
        if (tExpr == null) {
            importsDuringSearch.clear();
            return null;
        }

        env.imports.addAll(importsDuringSearch);
        importsDuringSearch.clear();

        if (tExpr.getExpression() instanceof SimpleName)
            return tExpr; /* found a variable in scope, just return it */
        if (isFunctionalInterface(targetType))
            return tExpr; /* synthesized code will be an anonymous class */

        /* assign a variable to this expression so that we don't have to search for it again in future */
        Assignment assignment = ast.newAssignment();
        assignment.setLeftHandSide(env.addVariable(targetType).getExpression()); // just the variable name
        assignment.setOperator(Assignment.Operator.ASSIGN);
        assignment.setRightHandSide(tExpr.getExpression());

        ParenthesizedExpression parenExpr = ast.newParenthesizedExpression();
        parenExpr.setExpression(assignment);
        return new TypedExpression(parenExpr, tExpr.getType());
    }

    private TypedExpression search(Type targetType, int argDepth) {
        if (argDepth > MAX_ARGUMENT_DEPTH)
            return null;

        /* see if a variable with the type already exists in scope */
        List<Variable> toSearch = new ArrayList<>(env.getScope().getVariables());
        sortVariablesByCost(toSearch);
        for (Variable v : toSearch)
            if (targetType.isAssignableFrom(v.getType())) {
                v.addRefCount();
                return new TypedExpression(ast.newSimpleName(v.getName()), v.getType());
            }

        /* check if this is a functional interface */
        if (isFunctionalInterface(targetType))
            return new TypedExpression(createAnonymousClass(targetType), targetType);

        /* could not pick variable, so concretize target type and resort to enumerative search */
        targetType.concretizeType(env);
        return enumerate(targetType, argDepth, toSearch);
    }

    private TypedExpression enumerate(Type targetType, int argDepth, List<Variable> toSearch) {
        Enumerator enumerator = new Enumerator(ast, env);

        /* first, see if we can create a new object of target type directly */
        List<Executable> constructors = new ArrayList<>(Arrays.asList(targetType.C().getConstructors()));
        /* static methods that return the target type are considered "constructors" here */
        for (Method m : targetType.C().getMethods())
            if (Modifier.isStatic(m.getModifiers()) && targetType.isAssignableFrom(m.getReturnType()))
                constructors.add(m);
        sortExecutablesByCost(constructors);
        for (Executable constructor : constructors) {
            if (Modifier.isAbstract(targetType.C().getModifiers()))
                break;
            if (! Modifier.isPublic(constructor.getModifiers()))
                continue;

            if (constructor instanceof Constructor) { /* an actual constructor */
                ClassInstanceCreation creation = ast.newClassInstanceCreation();
                creation.setType(ast.newSimpleType(ast.newSimpleName(targetType.C().getSimpleName())));

                int i;
                enumerator.importsDuringSearch.clear();
                for (i = 0; i < constructor.getParameterTypes().length; i++) {
                    Class argType = constructor.getParameterTypes()[i];
                    TypedExpression tArg = enumerator.search(new Type(argType), argDepth + 1);
                    if (tArg == null)
                        break;
                    creation.arguments().add(tArg.getExpression());
                }
                if (i == constructor.getParameterCount()) {
                    importsDuringSearch.addAll(enumerator.importsDuringSearch);
                    importsDuringSearch.add(targetType.C());
                    return new TypedExpression(creation, targetType);
                }
            }
            else { /* a static method that returns the object type */
                MethodInvocation invocation = ast.newMethodInvocation();

                int i;
                enumerator.importsDuringSearch.clear();
                invocation.setExpression(ast.newSimpleName(targetType.C().getSimpleName()));
                invocation.setName(ast.newSimpleName(constructor.getName()));
                for (i = 0; i < constructor.getParameterTypes().length; i++) {
                    Class argType = constructor.getParameterTypes()[i];
                    TypedExpression tArg = enumerator.search(new Type(argType), argDepth + 1);
                    if (tArg == null)
                        break;
                    invocation.arguments().add(tArg.getExpression());
                }
                if (i == constructor.getParameterCount()) {
                    importsDuringSearch.addAll(enumerator.importsDuringSearch);
                    importsDuringSearch.add(targetType.C());
                    return new TypedExpression(invocation, targetType);
                }
            }
        }

        /* otherwise, start recursive search for expression of target type */
        List<InvocationChain> chains = new ArrayList<>();
        for (Variable var : toSearch)
            chains.addAll(searchForChains(targetType, var));
        sortChainsByCost(chains);

        int i, j;
        for (InvocationChain chain : chains) {
            /* for each chain, see if we can synthesize all arguments in all methods in the chain */
            MethodInvocation invocation = ast.newMethodInvocation();
            Expression expr = ast.newSimpleName(chain.var.getName());
            enumerator.importsDuringSearch.clear();
            for (i = 0; i < chain.methods.size(); i++) {
                Method m = chain.methods.get(i);
                invocation.setExpression(expr);
                invocation.setName(ast.newSimpleName(m.getName()));

                for (j = 0; j < m.getParameterTypes().length; j++) {
                    Class argType  = m.getParameterTypes()[j];
                    TypedExpression  tArg;
                    try {
                        tArg = enumerator.search(new Type(argType), argDepth + 1);
                    } catch (SynthesisException e) {
                        break; // could not synthesize some argument, ignore this chain
                    }
                    if (tArg == null)
                        break;
                    invocation.arguments().add(tArg.getExpression());
                }
                if (j != m.getParameterCount())
                    break;
                expr = invocation;
                invocation = ast.newMethodInvocation();
            }
            if (i == chain.methods.size()) {
                importsDuringSearch.addAll(enumerator.importsDuringSearch);
                return new TypedExpression(expr, targetType);
            }
        }

        return null;
    }

    /* returns a list of method call chains that all produce the target type
     * TODO : use a memoizer to prune even more of the search space */
    private List<InvocationChain> searchForChains(Type targetType, Variable var) {
        List<InvocationChain> chains = new ArrayList<>();
        searchForChains(targetType, new InvocationChain(var), chains, 0);
        return chains;
    }

    private void searchForChains(Type targetType, InvocationChain chain, List<InvocationChain> chains, int composeLength) {
        Type currType = chain.getCurrentType();
        if (composeLength >= MAX_COMPOSE_LENGTH || currType.C().isPrimitive())
            return;
        List<Method> methods = Arrays.asList(currType.C().getMethods());
        sortMethodsByCost(methods);
        for (Method m : methods) {
            if (! Modifier.isPublic(m.getModifiers()))
                continue;
            try {
                chain.addMethod(m);
            } catch (SynthesisException e) {
                continue; // some problem with adding this method to chain, so ignore it
            }
            if (targetType.isAssignableFrom(chain.getCurrentType()))
                chains.add(new InvocationChain(chain));
            else
                searchForChains(targetType, chain, chains, composeLength+1);
            chain.pop();
        }
    }

    public Type searchType() {
        List<Variable> vars = new ArrayList<>(env.getScope().getVariables());
        List<Type> types = vars.stream().map(v -> v.getType())
                .filter(t -> t.T().isSimpleType())
                .collect(Collectors.toList()); // only consider simple types

        sortTypesByCost(types);
        if (types.isEmpty())
            throw new SynthesisException(SynthesisException.TypeNotFoundDuringSearch);
        Type type = types.get(0);
        type.addRefCount();
        return type;
    }

    private void sortTypesByCost(List<Type> types) {
        Collections.shuffle(types);
        types.sort(Comparator.comparingInt(t -> t.refCount));
    }

    private void sortConstructorsByCost(List<Constructor> constructors) {
        Collections.shuffle(constructors);
        constructors.sort(Comparator.comparingInt(c -> c.getParameterTypes().length));
    }

    private void sortMethodsByCost(List<Method> methods) {
        Collections.shuffle(methods);
        methods.sort(Comparator.comparingInt(c -> c.getParameterTypes().length));
    }

    private void sortExecutablesByCost(List<Executable> exes) {
        Collections.shuffle(exes);
        exes.sort(Comparator.comparingInt(e -> e.getParameterTypes().length));
    }

    private void sortVariablesByCost(List<Variable> variables) {
        variables.sort(Comparator.comparingInt(v -> v.getRefCount()));
    }

    private void sortChainsByCost(List<InvocationChain> chains) {
        chains.sort(Comparator.comparingInt(chain -> chain.structCost()));
    }

    private boolean isFunctionalInterface(Type type) {
        return type.C().isInterface() && type.C().getMethods().length == 1;
    }

    private ClassInstanceCreation createAnonymousClass(Type targetType) {
        ClassInstanceCreation creation = ast.newClassInstanceCreation();
        creation.setType(ast.newSimpleType(ast.newSimpleName(targetType.C().getSimpleName())));
        AnonymousClassDeclaration anonymousClass = ast.newAnonymousClassDeclaration();
        creation.setAnonymousClassDeclaration(anonymousClass);

        /* TODO: synthesize a stub of the (one) method in functional interface */
        importsDuringSearch.add(targetType.C());
        return creation;
    }

}
