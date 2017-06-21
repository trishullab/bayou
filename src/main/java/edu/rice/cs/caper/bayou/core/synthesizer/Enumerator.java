package edu.rice.bayou.synthesizer;

import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class Enumerator {

    final AST ast;
    final Environment env;
    static final int MAX_COMPOSE_LENGTH = 3; // a().b().c().d()...
    static final int MAX_ARGUMENT_DEPTH = 2; // a(b(c(d(...))))
    static final int K = 3; // number of arguments is given K times more weight than length of composition in cost

    class InvocationChain {
        final Variable var;
        final List<Method> methods;

        InvocationChain(Variable var) {
            this.var = var;
            methods = new ArrayList<>();
        }

        InvocationChain(InvocationChain chain) {
            var = chain.var;
            methods = new ArrayList<>(chain.methods);
        }

        void addMethod(Method m) {
            methods.add(m);
        }

        void pop() {
            methods.remove(methods.size()-1);
        }

        Class getCurrentType() {
            return methods.isEmpty()? var.getType() : methods.get(methods.size()-1).getReturnType();
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

    public Expression search(Class targetType) {
        Expression expr = search(targetType, 0);
        if (expr == null) {
            importsDuringSearch.clear();
            return null;
        }

        env.imports.addAll(importsDuringSearch);
        importsDuringSearch.clear();

        if (expr instanceof SimpleName)
            return expr; /* found a variable in scope, just return it */
        if (isFunctionalInterface(targetType))
            return expr; /* synthesized code will be an anonymous class */

        /* assign a variable to this expression so that we don't have to search for it again in future */
        Assignment assignment = ast.newAssignment();
        assignment.setLeftHandSide(env.addVariable(targetType));
        assignment.setOperator(Assignment.Operator.ASSIGN);
        assignment.setRightHandSide(expr);

        ParenthesizedExpression parenExpr = ast.newParenthesizedExpression();
        parenExpr.setExpression(assignment);
        return parenExpr;
    }

    private Expression search(Class targetType, int argDepth) {
        if (argDepth > MAX_ARGUMENT_DEPTH)
            return null;
        Expression expr = null;

        /* see if a variable with the type already exists in scope */
        List<Variable> toSearch = new ArrayList<>(env.scope);
        toSearch.addAll(env.mu_scope);
        sortVariablesByCost(toSearch);
        for (Variable v : toSearch)
            if (targetType.isAssignableFrom(v.getType())) {
                expr = ast.newSimpleName(v.getName());
                v.addRefCount();
                break;
            }

        /* could not pick variable, resort to enumerative search */
        if (expr == null) {
            if (isFunctionalInterface(targetType))
                return createAnonymousClass(targetType); /* but first, check if this is a functional interface */

            expr = enumerate(targetType, argDepth, toSearch);
        }

        return expr;
    }

    private Expression enumerate(Class targetType, int argDepth, List<Variable> toSearch) {
        Enumerator enumerator = new Enumerator(ast, env);

        /* first, see if we can create a new object of target type directly */
        List<Executable> constructors = new ArrayList<>(Arrays.asList(targetType.getConstructors()));
        /* static methods that return the target type are considered "constructors" here */
        try {
            for (Method m : targetType.getMethods())
                if (Modifier.isStatic(m.getModifiers()) && targetType.isAssignableFrom(m.getReturnType()))
                    constructors.add(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sortExecutablesByCost(constructors);
        for (Executable constructor : constructors) {
            if (Modifier.isAbstract(targetType.getModifiers()))
                break;
            if (! Modifier.isPublic(constructor.getModifiers()))
                continue;

            if (constructor instanceof Constructor) { /* an actual constructor */
                ClassInstanceCreation creation = ast.newClassInstanceCreation();
                creation.setType(ast.newSimpleType(ast.newSimpleName(targetType.getSimpleName())));

                int i;
                enumerator.importsDuringSearch.clear();
                for (i = 0; i < constructor.getParameterTypes().length; i++) {
                    Class argType = constructor.getParameterTypes()[i];
                    Expression arg = enumerator.search(argType, argDepth + 1);
                    if (arg == null)
                        break;
                    creation.arguments().add(arg);
                }
                if (i == constructor.getParameterCount()) {
                    importsDuringSearch.addAll(enumerator.importsDuringSearch);
                    importsDuringSearch.add(targetType);
                    return creation;
                }
            }
            else { /* a static method that returns the object type */
                MethodInvocation invocation = ast.newMethodInvocation();
                Expression expr = ast.newSimpleName(targetType.getSimpleName());

                int i;
                enumerator.importsDuringSearch.clear();
                invocation.setExpression(ast.newSimpleName(targetType.getSimpleName()));
                invocation.setName(ast.newSimpleName(constructor.getName()));
                for (i = 0; i < constructor.getParameterTypes().length; i++) {
                    Class argType = constructor.getParameterTypes()[i];
                    Expression arg = enumerator.search(argType, argDepth + 1);
                    if (arg == null)
                        break;
                    invocation.arguments().add(arg);
                }
                if (i == constructor.getParameterCount()) {
                    importsDuringSearch.addAll(enumerator.importsDuringSearch);
                    importsDuringSearch.add(targetType);
                    return invocation;
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
                    Expression arg = enumerator.search(argType, argDepth+1);
                    if (arg == null)
                        break;
                    invocation.arguments().add(arg);
                }
                if (j != m.getParameterCount())
                    break;
                expr = invocation;
                invocation = ast.newMethodInvocation();
            }
            if (i == chain.methods.size()) {
                importsDuringSearch.addAll(enumerator.importsDuringSearch);
                return expr;
            }
        }

        return null;
    }

    /* returns a list of method call chains that all produce the target type
     * TODO : use a memoizer to prune even more of the search space */
    private List<InvocationChain> searchForChains(Class targetType, Variable var) {
        List<InvocationChain> chains = new ArrayList<>();
        searchForChains(targetType, new InvocationChain(var), chains, 0);
        return chains;
    }

    private void searchForChains(Class targetType, InvocationChain chain, List<InvocationChain> chains, int composeLength) {
        Class currType = chain.getCurrentType();
        if (composeLength >= MAX_COMPOSE_LENGTH || currType.isPrimitive())
            return;
        List<Method> methods = Arrays.asList(currType.getMethods());
        sortMethodsByCost(methods);
        for (Method m : methods) {
            if (! Modifier.isPublic(m.getModifiers()))
                continue;
            chain.addMethod(m);
            if (targetType.isAssignableFrom(chain.getCurrentType()))
                chains.add(new InvocationChain(chain));
            else
                searchForChains(targetType, chain, chains, composeLength+1);
            chain.pop();
        }
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
        variables.sort(Comparator.comparingInt(v -> v.refCount));
    }

    private void sortChainsByCost(List<InvocationChain> chains) {
        chains.sort(Comparator.comparingInt(chain -> chain.structCost()));
    }

    private boolean isFunctionalInterface(Class cls) {
        return cls.isInterface() && cls.getMethods().length == 1;
    }

    private ClassInstanceCreation createAnonymousClass(Class targetType) {
        ClassInstanceCreation creation = ast.newClassInstanceCreation();
        creation.setType(ast.newSimpleType(ast.newSimpleName(targetType.getSimpleName())));
        AnonymousClassDeclaration anonymousClass = ast.newAnonymousClassDeclaration();
        creation.setAnonymousClassDeclaration(anonymousClass);

        /* TODO: synthesize a stub of the (one) method in functional interface */
        importsDuringSearch.add(targetType);
        return creation;
    }

}
