package edu.rice.bayou.dsl;

import org.eclipse.jdt.core.dom.*;
import edu.rice.bayou.synthesizer.Environment;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DAPICall extends DASTNode {

    final String node = "DAPICall";
    final String _call;

    /* CAUTION: This field is only available during AST generation */
    final transient IMethodBinding methodBinding;
    /* CAUTION: These fields are only available during synthesis (after synthesize(...) is called) */
    transient Method method;
    transient Constructor constructor;

    /* TODO: Add refinement types (predicates) here */

    public DAPICall(IMethodBinding methodBinding) {
        this.methodBinding = methodBinding;
        this._call = getClassName() + "." + getSignature();
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max) throws TooManySequencesException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (Sequence sequence : soFar)
            sequence.addCall(_call);
    }

    private String getClassName() {
        String className = methodBinding.getDeclaringClass().getQualifiedName();
        if (className.contains("<")) /* be agnostic to generic versions */
            className = className.substring(0, className.indexOf("<"));
        return className;
    }

    private String getSignature() {
        Stream<String> types = Arrays.stream(methodBinding.getParameterTypes()).map(t -> t.getQualifiedName());
        return methodBinding.getName() + "(" + String.join(",", types.collect(Collectors.toCollection(ArrayList::new))) + ")";
    }

    @Override
    public int numStatements() {
        return 1;
    }

    @Override
    public int numLoops() {
        return 0;
    }

    @Override
    public int numBranches() {
        return 0;
    }

    @Override
    public int numExcepts() {
        return 0;
    }

    @Override
    public Set<DAPICall> bagOfAPICalls() {
        Set<DAPICall> bag = new HashSet<>();
        bag.add(this);
        return bag;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DAPICall))
            return false;
        DAPICall apiCall = (DAPICall) o;
        return _call.equals(apiCall._call);
    }

    @Override
    public int hashCode() {
        return _call.hashCode();
    }

    @Override
    public String toString() {
        return _call;
    }



    @Override
    public ASTNode synthesize(Environment env) {
        Executable executable = getConstructorOrMethod();
        if (executable instanceof Constructor) {
            constructor = (Constructor) executable;
            return synthesizeClassInstanceCreation(env);
        }
        else {
            method = (Method) executable;
            return synthesizeMethodInvocation(env);
        }
    }

    private Assignment synthesizeClassInstanceCreation(Environment env) {
        AST ast = env.ast();
        ClassInstanceCreation creation = ast.newClassInstanceCreation();

        /* constructor type */
        SimpleType t = ast.newSimpleType(ast.newName(constructor.getDeclaringClass().getSimpleName()));
        creation.setType(t);

        /* constructor arguments */
        for (Class type : constructor.getParameterTypes()) {
            Expression arg = env.searchOrAddVariable(type, true);
            creation.arguments().add(arg);
        }

        /* constructor return object */
        Expression ret = env.searchOrAddVariable(constructor.getDeclaringClass(), false);

        /* the assignment */
        Assignment assignment = ast.newAssignment();
        assignment.setLeftHandSide(ret);
        assignment.setRightHandSide(creation);
        assignment.setOperator(Assignment.Operator.ASSIGN);

        /* update environment of exceptions */
        for (Class thrown : constructor.getExceptionTypes())
            env.recordExceptionThrown(thrown);

        return assignment;
    }

    private ASTNode synthesizeMethodInvocation(Environment env) {
        AST ast = env.ast();
        MethodInvocation invocation = ast.newMethodInvocation();

        /* method name */
        SimpleName metName = ast.newSimpleName(method.getName());
        invocation.setName(metName);

        /* object on which method is invoked */
        Expression object = env.searchOrAddVariable(method.getDeclaringClass(), true);
        invocation.setExpression(object);

        /* method arguments */
        for (Class type : method.getParameterTypes()) {
            Expression arg = env.searchOrAddVariable(type, true);
            invocation.arguments().add(arg);
        }

        /* update environment of exceptions */
        for (Class thrown : method.getExceptionTypes())
            env.recordExceptionThrown(thrown);

        if (method.getReturnType().equals(void.class))
            return invocation;

        /* method return value */
        Expression ret = env.searchOrAddVariable(method.getReturnType(), false);

        /* the assignment */
        Assignment assignment = ast.newAssignment();
        assignment.setLeftHandSide(ret);
        assignment.setRightHandSide(invocation);
        assignment.setOperator(Assignment.Operator.ASSIGN);

        return assignment;
    }

    private Executable getConstructorOrMethod() {
        String qualifiedName = _call.substring(0, _call.indexOf("("));
        String className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        Class cls = Environment.getClass(className);

        /* find the method in the class */
        for (Method m : cls.getMethods()) {
            String name = null;
            for (String s : m.toString().split(" "))
                if (s.contains("(")) {
                    name = s;
                    break;
                }
            if (name != null && name.replaceAll("\\$", ".").equals(_call))
                return m;
        }

        /* .. or the constructor */
        String _callC = className + _call.substring(_call.indexOf("("));
        for (Constructor c : cls.getConstructors()) {
            String name = null;
            for (String s : c.toString().split(" "))
                if (s.contains("(")) {
                    name = s;
                    break;
                }
            if (name != null && name.replaceAll("\\$", ".").equals(_callC))
                return c;
        }

        System.err.println("Could not find methodBinding or constructor " + qualifiedName);
        System.exit(1);
        return null;
    }
}
