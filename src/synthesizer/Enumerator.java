package synthesizer;

import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class Enumerator {

    final AST ast;
    final Environment env;
    static final int MAX_COMPOSE_LENGTH = 2; // a().b().c().d()...
    static final int MAX_ARGUMENT_DEPTH = 1; // a(b(c(d(...))))

    class EnumeratorDataStructure {
        final Class type;
        final Expression expr;
        final int composeLength;

        EnumeratorDataStructure(Class type, Expression expr, int composeLength) {
            this.type = type;
            this.expr = expr;
            this.composeLength = composeLength;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || ! (o instanceof EnumeratorDataStructure))
                return false;
            return this.type.equals(((EnumeratorDataStructure) o).type);
        }

        @Override
        public int hashCode() {
            return type.hashCode();
        }
    }

    private final Queue<EnumeratorDataStructure> queue;
    private final List<Class> importsDuringSearch;

    public Enumerator(AST ast, Environment env) {
        this.ast = ast;
        this.env = env;
        this.queue = new LinkedList<>();
        this.importsDuringSearch = new ArrayList<>();
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

            List<Variable> scope = new ArrayList<>(env.scope);
            scope.addAll(env.mu_scope);
            for (Variable var : scope) {
                SimpleName varExpr = ast.newSimpleName(var.getName());
                EnumeratorDataStructure e = new EnumeratorDataStructure(var.getType(), varExpr, 0);
                queue.add(e);
            }
            expr = enumerate(targetType, argDepth);
            queue.clear();
        }

        return expr;
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

    private Expression enumerate(Class targetType, int argDepth) {
        Enumerator enumerator = new Enumerator(ast, env);

        /* first, see if we can create a new object of targetType directly */
        List<Constructor> constructors = Arrays.asList(targetType.getConstructors());
        sortConstructorsByCost(constructors);
        for (Constructor constructor : constructors) {
            if (! Modifier.isPublic(constructor.getModifiers()))
                continue;
            ClassInstanceCreation creation = ast.newClassInstanceCreation();
            creation.setType(ast.newSimpleType(ast.newSimpleName(targetType.getSimpleName())));
            boolean allArgsAdded = true;

            for (Class argType : constructor.getParameterTypes()) {
                Expression arg = enumerator.search(argType, argDepth+1);
                if (arg == null) {
                    allArgsAdded = false;
                    break;
                }
                creation.arguments().add(arg);
            }
            if (allArgsAdded)
                return creation;
        }

        /* otherwise, start breadth-first recursive search for targetType from methods of types in queue */
        while (! queue.isEmpty()) {
            EnumeratorDataStructure e = queue.poll();
            List<Method> methods = Arrays.asList(e.type.getMethods());
            sortMethodsByCost(methods);

            for (Method method : methods) {
                if (! Modifier.isPublic(method.getModifiers()))
                    continue;
                Class returnType = method.getReturnType();

                MethodInvocation invocation = ast.newMethodInvocation();
                invocation.setName(ast.newSimpleName(method.getName()));
                Expression expr = (Expression) ASTNode.copySubtree(ast, e.expr); // cannot reuse the same node in AST
                invocation.setExpression(expr);

                boolean allArgsAdded = true;
                for (Class argType : method.getParameterTypes()) {
                    Expression arg = enumerator.search(argType, argDepth+1);
                    if (arg == null) {
                        allArgsAdded = false;
                        break;
                    }
                    invocation.arguments().add(arg);
                }

                if (allArgsAdded) {
                    if (returnType.equals(targetType))
                        return invocation;
                    else if (e.composeLength < MAX_COMPOSE_LENGTH)
                        queue.add(new EnumeratorDataStructure(returnType, invocation, e.composeLength + 1));
                }
            }
        }

        return null;
    }

    private void sortConstructorsByCost(List<Constructor> constructors) {
        Collections.shuffle(constructors);
        constructors.sort(Comparator.comparingInt(c -> c.getParameterTypes().length));
    }

    private void sortMethodsByCost(List<Method> methods) {
        Collections.shuffle(methods);
        methods.sort(Comparator.comparingInt(c -> c.getParameterTypes().length));
    }

    private void sortVariablesByCost(List<Variable> variables) {
        variables.sort(Comparator.comparingInt(v -> v.refCount));
    }
}
