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

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

public class Enumerator {

    final AST ast;
    final Environment env;
    Synthesizer.Mode mode;

    public Enumerator(AST ast, Environment env, Synthesizer.Mode mode) {
        this.ast = ast;
        this.env = env;
        this.mode = mode;
    }

    public TypedExpression search(SearchTarget target) {
        TypedExpression tExpr = search(target, 0);
        if (tExpr == null)
            return null;

        if (tExpr.getExpression() instanceof SimpleName)
            return tExpr; /* found a variable in scope, just return it */

        /* assign a variable to this expression so that we don't have to search for it again in future */
        Assignment assignment = ast.newAssignment();
        VariableProperties properties = new VariableProperties()
                .setJoin(true)
                .setDefaultInit(false)
                .setSingleUse(target.getSingleUseVariable());
        TypedExpression expr = target.getParamName() != null?
                env.addVariable(new Variable(target.getParamName(), target.getType(), properties)) :
                env.addVariable(target.getType(), properties);
        assignment.setLeftHandSide(expr.getExpression()); // just the variable name
        assignment.setOperator(Assignment.Operator.ASSIGN);
        assignment.setRightHandSide(tExpr.getExpression());

        ParenthesizedExpression parenExpr = ast.newParenthesizedExpression();
        parenExpr.setExpression(assignment);
        return new TypedExpression(parenExpr, tExpr.getType())
                .addReferencedVariables(tExpr.getReferencedVariables())
                .addReferencedVariables(expr.getReferencedVariables())
                .addAssociatedImports(tExpr.getAssociatedImports())
                .addAssociatedImports(expr.getAssociatedImports());
    }

    private TypedExpression search(SearchTarget target, int argDepth) {
        if (argDepth > ExpressionChain.MAX_ARGUMENT_DEPTH)
            return null;

        /* see if a variable with the type already exists in scope */
        List<Variable> toSearch = new ArrayList<>(env.getScope().getVariables());
        sortVariablesByCost(toSearch, target);
        for (Variable v : toSearch)
            if (!v.isSingleUseVar() && target.getType().isAssignableFrom(v.getType())) {
                v.addRefCount();
                return new TypedExpression(v.createASTNode(ast), v.getType())
                        .addReferencedVariable(v).addAssociatedImport(v.getType().C());
            }

        /* could not pick variable, so concretize target type */
        target.getType().concretizeType(env);

        /* ... and start enumerative search or, if enumeration failed, add variable (with default init) */
        TypedExpression expr = enumerate(target, argDepth, toSearch);
        if (expr == null) {
            VariableProperties properties = new VariableProperties()
                                                            .setJoin(true)
                                                            .setDefaultInit(true)
                                                            .setSingleUse(target.getSingleUseVariable());
            if (target.getParamName() != null) { // create variable with name here, but note that it may be refactored
                Variable var = new Variable(target.getParamName(), target.getType(), properties);
                expr = env.addVariable(var);
            }
            else {
                expr = env.addVariable(target.getType(), properties);
            }
        }

        return expr;
    }

    private TypedExpression enumerate(SearchTarget target, int argDepth, List<Variable> toSearch) {
        Enumerator enumerator = new Enumerator(ast, env, mode);
        Type targetType = target.getType();

        // if a predefined constant exists for this type, just use it
        if (env.predefinedConstants.hasPredefinedConstant(targetType.C()))
            return env.predefinedConstants.getTypedExpression(targetType.C(), ast);

        /* first, see if we can create a new object of target type directly */
        List<Executable> constructors = new ArrayList<>(Arrays.asList(targetType.C().getConstructors()));
        /* static methods that return the target type are considered "constructors" here */
        for (Method m : targetType.C().getMethods())
            if (Modifier.isStatic(m.getModifiers()) && targetType.isAssignableFrom(m.getReturnType()))
                constructors.add(m);
        sortExecutablesByCost(constructors);

        List<TypedExpression> candidates = new ArrayList<>();
        for (Executable constructor : constructors) {
            if (Modifier.isAbstract(targetType.C().getModifiers())
                    || Modifier.isInterface(targetType.C().getModifiers())) {
                for (Object o : env.reflections.getSubTypesOf(targetType.C())) {
                    TypedExpression tExpr = enumerator.enumerate(
                            new SearchTarget(new Type((Class) o)), argDepth, toSearch);
                    if (tExpr != null) {
                        // concretize the type before finalizing candidate
                        tExpr.getType().concretizeType(env);
                        candidates.add(tExpr);
                    }
                }
                break;
            }
            if (! Modifier.isPublic(constructor.getModifiers()))
                continue;

            if (constructor instanceof Constructor) { /* an actual constructor */
                ClassInstanceCreation creation = ast.newClassInstanceCreation();
                TypedExpression tExpr = new TypedExpression(creation, targetType);
                creation.setType(ast.newSimpleType(ast.newSimpleName(targetType.C().getSimpleName())));

                int i;
                for (i = 0; i < constructor.getParameterCount(); i++) {
                    Class argType = constructor.getParameterTypes()[i];
                    String name = constructor.getParameters()[i].getName();
                    SearchTarget newTarget = new SearchTarget(new Type(argType))
                            .setAPICallName(constructor.getName())
                            .setParamName(name)
                            .setSingleUseVariable(true);
                    TypedExpression tArg = enumerator.search(newTarget, argDepth + 1);
                    if (tArg == null)
                        break;
                    tExpr.addReferencedVariables(tArg.getReferencedVariables());
                    tExpr.addAssociatedImports(tArg.getAssociatedImports());
                    creation.arguments().add(tArg.getExpression());
                }
                if (i == constructor.getParameterCount()) {
                    tExpr.addAssociatedImport(targetType.C());
                    candidates.add(tExpr);
                }
            }
            else { /* a static method that returns the object type */
                MethodInvocation invocation = ast.newMethodInvocation();
                TypedExpression tExpr = new TypedExpression(invocation, targetType);
                int i;
                invocation.setExpression(ast.newSimpleName(targetType.C().getSimpleName()));
                invocation.setName(ast.newSimpleName(constructor.getName()));
                for (i = 0; i < constructor.getParameterCount(); i++) {
                    Class argType = constructor.getParameterTypes()[i];
                    String name = constructor.getParameters()[i].getName();
                    SearchTarget newTarget = new SearchTarget(new Type(argType))
                            .setAPICallName(constructor.getName())
                            .setParamName(name)
                            .setSingleUseVariable(true);
                    TypedExpression tArg = enumerator.search(newTarget, argDepth + 1);
                    if (tArg == null)
                        break;
                    tExpr.addReferencedVariables(tArg.getReferencedVariables());
                    tExpr.addAssociatedImports(tArg.getAssociatedImports());
                    invocation.arguments().add(tArg.getExpression());
                }
                if (i == constructor.getParameterCount()) {
                    tExpr.addAssociatedImport(targetType.C());
                    candidates.add(tExpr);
                }
            }
        }

        if (! candidates.isEmpty()) {
            // sort according to cost heuristic and get the top-most one
            sortTypedExpressionsByCost(candidates);
            TypedExpression retExpr = candidates.get(0);

            // remove all $ variables added by OTHER candidates
            for (int i = 1; i < candidates.size(); i++)
                for (Variable var : candidates.get(i).getReferencedVariables())
                    if (var.isDefaultInit() && var.getRefCount() == 0)
                        env.removeVariable(var);

            return retExpr;
        }

        if (mode == Synthesizer.Mode.CONDITIONAL_PROGRAM_GENERATOR) {
            return null;
        }

        /* otherwise, start recursive search for expression of target type */
        List<ExpressionChain> chains = new ArrayList<>();
        for (Variable var : toSearch)
            chains.addAll(searchForChains(targetType, var));
        sortChainsByCost(chains);

        int i, j;
        for (ExpressionChain chain : chains) {
            /* for each chain, see if we can synthesize all arguments in all methods in the chain */
            MethodInvocation invocation = ast.newMethodInvocation();
            Expression expr = chain.var.createASTNode(ast);
            Set<Variable> referencedVariables = new HashSet<>();
            Set<Class> associatedImports = new HashSet<>();
            for (i = 0; i < chain.methods.size(); i++) {
                Method m = chain.methods.get(i);
                invocation.setExpression(expr);
                invocation.setName(ast.newSimpleName(m.getName()));

                for (j = 0; j < m.getParameterCount(); j++) {
                    Class argType  = m.getParameterTypes()[j];
                    String name = m.getParameters()[j].getName();
                    SearchTarget newTarget = new SearchTarget(new Type(argType))
                            .setAPICallName(m.getName())
                            .setParamName(name)
                            .setSingleUseVariable(true);
                    TypedExpression  tArg;
                    try {
                        tArg = enumerator.search(newTarget, argDepth + 1);
                    } catch (SynthesisException e) {
                        break; // could not synthesize some argument, ignore this chain
                    }
                    if (tArg == null)
                        break;
                    referencedVariables.addAll(tArg.getReferencedVariables());
                    associatedImports.addAll(tArg.getAssociatedImports());
                    invocation.arguments().add(tArg.getExpression());
                }
                if (j != m.getParameterCount())
                    break;
                expr = invocation;
                invocation = ast.newMethodInvocation();
            }
            if (i == chain.methods.size()) {
                return new TypedExpression(expr, targetType)
                        .addReferencedVariables(referencedVariables)
                        .addAssociatedImports(associatedImports);
            }
        }

        return null;
    }

    /* returns a list of method call chains that all produce the target type */
    private List<ExpressionChain> searchForChains(Type targetType, Variable var) {
        List<ExpressionChain> chains = new ArrayList<>();
        searchForChains(targetType, new ExpressionChain(var), chains, 0);
        return chains;
    }

    private void searchForChains(Type targetType, ExpressionChain chain, List<ExpressionChain> chains, int composeLength) {
        Type currType = chain.getCurrentType();
        if (composeLength >= ExpressionChain.MAX_COMPOSE_LENGTH || currType.C().isPrimitive())
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
                chains.add(new ExpressionChain(chain));
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
        if (types.isEmpty()) {
            return new Type(ast.newSimpleType(ast.newName("java.lang.String")), String.class); // String is the default type
        }
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

    private void sortVariablesByCost(List<Variable> variables, SearchTarget target) {
        String compareWith = "";
        if (target.getParamName() != null)
            compareWith += target.getParamName().toLowerCase();
        if (target.getAPICallName() != null)
            compareWith += target.getAPICallName().toLowerCase();

        if (compareWith.equals(""))
            variables.sort(Comparator.comparingInt(v -> v.getRefCount()));
        else {
            String compare = compareWith;
            variables.sort(Comparator.comparingInt(v ->
                    LevenshteinDistance.getDefaultInstance().apply(compare, v.getName())));
        }

        // give more precedence to user-defined variables by moving them to the front
        List<Variable> precedenced = new ArrayList<>();
        for (Variable v : variables)
            if (v.isUserVar())
                precedenced.add(v);
        for (Variable v : variables)
            if (!v.isUserVar())
                precedenced.add(v);
        for (int i = 0; i < precedenced.size(); i++)
            variables.set(i, precedenced.get(i));
    }

    private void sortChainsByCost(List<ExpressionChain> chains) {
        chains.sort(Comparator.comparingInt(chain -> chain.structCost()));
    }

    private void sortTypedExpressionsByCost(List<TypedExpression> exprs) {
        ToIntFunction<TypedExpression> key = new ToIntFunction<TypedExpression>() {
            @Override
            public int applyAsInt(TypedExpression expr) {
                Set<Variable> referencedVariables = expr.getReferencedVariables();
                int c = 0;
                for (Variable var : referencedVariables)
                    if (var.isUserVar())
                        c++;
                return c;
            }
        };
        exprs.sort(Comparator.comparingInt(key).reversed());
    }
}
