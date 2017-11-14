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
package edu.rice.cs.caper.bayou.core.dsl;


import edu.rice.cs.caper.bayou.core.synthesizer.*;
import edu.rice.cs.caper.bayou.core.synthesizer.Type;
import org.eclipse.jdt.core.dom.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DAPICall extends DASTNode
{

    String node = "DAPICall";
    String _call;
    transient String retVarName = "";
    
    /* CAUTION: This field is only available during AST generation */
    transient IMethodBinding methodBinding;
    transient int linenum;
    /* CAUTION: These fields are only available during synthesis (after synthesize(...) is called) */
    transient Method method;
    transient Constructor constructor;

    /* TODO: Add refinement types (predicates) here */

    public DAPICall() {
        this._call = "";
        this.node = "DAPICall";
    }

    public DAPICall(IMethodBinding methodBinding, int linenum) {
        this.methodBinding = methodBinding;
        this._call = getClassName() + "." + getSignature();
        this.linenum = linenum;
        this.node = "DAPICall";
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length) throws TooManySequencesException, TooLongSequenceException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (Sequence sequence : soFar) {
            sequence.addCall(_call);
            if (sequence.getCalls().size() > max_length)
                throw new TooLongSequenceException();
        }
    }

    private String getClassName() {
        ITypeBinding cls = methodBinding.getDeclaringClass();
        String className = cls.getQualifiedName();
        if (cls.isGenericType())
            className += "<" + String.join(",", Arrays.stream(cls.getTypeParameters()).map(
                    t -> (t.isTypeVariable()? "Tau_": "") + t.getName()
            ).collect(Collectors.toList())) + ">";
        return className;
    }

    private String getSignature() {
        Stream<String> types = Arrays.stream(methodBinding.getParameterTypes()).map(
                t -> (t.isTypeVariable()? "Tau_": "") + t.getQualifiedName());
        return methodBinding.getName() + "(" + String.join(",", types.collect(Collectors.toCollection(ArrayList::new))) + ")";
    }

    public void setNotPredicate() {
        this._call = "$NOT$" + this._call;
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
    public Set<Class> exceptionsThrown() {
        if (constructor != null)
            return new HashSet<>(Arrays.asList(constructor.getExceptionTypes()));
        else
            return new HashSet<>(Arrays.asList(method.getExceptionTypes()));
    }

    @Override
    public Set<Class> exceptionsThrown(Set<String> eliminatedVars) {
        if (!eliminatedVars.contains(this.retVarName))
            return this.exceptionsThrown();
        else
            return new HashSet<>();
    }

    public String getRetVarName() {
        return this.retVarName;
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
    public ASTNode synthesize(Environment env) throws SynthesisException
    {
        boolean notPredicate = _call.contains("$NOT$");
        if (notPredicate)
            _call = _call.replaceAll("\\$NOT\\$", "");
        Executable executable = getConstructorOrMethod();
        if (executable instanceof Constructor) {
            constructor = (Constructor) executable;
            return synthesizeClassInstanceCreation(env);
        }
        else {
            method = (Method) executable;
            return synthesizeMethodInvocation(env, notPredicate);
        }
    }

    private Assignment synthesizeClassInstanceCreation(Environment env) throws SynthesisException {
        AST ast = env.ast();
        ClassInstanceCreation creation = ast.newClassInstanceCreation();

        /* constructor type */
        Type type = new Type(constructor.getDeclaringClass());
        type.concretizeType(env);
        creation.setType(type.T());

        /* constructor arguments */
        for (java.lang.reflect.Type gType : constructor.getGenericParameterTypes()) {
            Type argType = type.getConcretization(gType);
            TypedExpression arg = env.search(argType);
            creation.arguments().add(arg.getExpression());
        }

        /* constructor return object */
        TypedExpression ret = env.addVariable(type);

        /* the assignment */
        Assignment assignment = ast.newAssignment();
        assignment.setLeftHandSide(ret.getExpression());
        assignment.setRightHandSide(creation);
        assignment.setOperator(Assignment.Operator.ASSIGN);

        // Record the returned variable name
        if (ret.getExpression() instanceof SimpleName)
            this.retVarName = ret.getExpression().toString();

        return assignment;
    }

    private ASTNode synthesizeMethodInvocation(Environment env, boolean toBeNegated) throws SynthesisException {
        AST ast = env.ast();
        MethodInvocation invocation = ast.newMethodInvocation();

        /* method name */
        SimpleName metName = ast.newSimpleName(method.getName());
        invocation.setName(metName);

        /* object on which method is invoked */
        TypedExpression object = env.search(new Type(method.getDeclaringClass()));
        invocation.setExpression(object.getExpression());

        /* concretizeType method argument types using the above object and search for them */
        for (java.lang.reflect.Type gType : method.getGenericParameterTypes()) {
            Type argType = object.getType().getConcretization(gType);
            TypedExpression arg = env.search(argType);
            invocation.arguments().add(arg.getExpression());
        }

        if (method.getReturnType().equals(void.class))
            return invocation;

        /* method return value */
        Type retType = object.getType().getConcretization(method.getGenericReturnType());
        TypedExpression ret = env.addVariable(retType);

        /* the assignment */
        Expression lhs = ret.getExpression();
        Expression rhs;
        if (toBeNegated) {
            rhs = ast.newPrefixExpression();
            ((PrefixExpression) rhs).setOperator(PrefixExpression.Operator.NOT);
            ((PrefixExpression) rhs).setOperand(invocation);
        } else
            rhs = invocation;
        Assignment assignment = ast.newAssignment();
        assignment.setLeftHandSide(lhs);
        assignment.setRightHandSide(rhs);
        assignment.setOperator(Assignment.Operator.ASSIGN);

        return assignment;
    }

    private Executable getConstructorOrMethod() throws SynthesisException {
        /* get the type-erased name */
        String qualifiedName = _call.substring(0, _call.indexOf("("));
        String[] args = _call.substring(_call.indexOf("(") + 1, _call.lastIndexOf(")")).split(",");

        String className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        String methodName = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
        String erasedClassName = className.replaceAll("<.*>", "");
        Class cls = Environment.getClass(erasedClassName);

        TypeVariable[] typeVars = cls.getTypeParameters();
        List<String> erasedArgs = new ArrayList<>();
        for (String arg : args) {
            if (! arg.startsWith("Tau_")) {
                erasedArgs.add(arg);
                continue;
            }
            // generic type variable
            String typeVarName = arg.substring(4);
            TypeVariable typeVar = null;
            for (TypeVariable t : typeVars)
                if (t.getName().equals(typeVarName)) {
                    typeVar = t;
                    break;
                }
            if (typeVar == null)
                throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);
            java.lang.reflect.Type bound = typeVar.getBounds()[0]; // first bound is the class
            erasedArgs.add(((Class) bound).getName());
        }
        String erasedName = erasedClassName + "." + methodName + "(" + String.join(",", erasedArgs) + ")";

        /* find the method in the class */
        for (Method m : cls.getMethods()) {
            String name = null;
            for (String s : m.toString().split(" "))
                if (s.contains("(")) {
                    name = s;
                    break;
                }
            if (name != null && name.replaceAll("\\$", ".").equals(erasedName))
                return m;
        }

        /* .. or the constructor */
        String _callC = erasedClassName + erasedName.substring(erasedName.indexOf("("));
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

        throw new SynthesisException(SynthesisException.MethodOrConstructorNotFound);
    }
}
