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

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DAPICall extends DASTNode
{

    public class InvalidAPICallException extends Exception {}

    String node = "DAPICall";
    String _call;
    List<String> _throws;
    String _returns;
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

    public DAPICall(IMethodBinding methodBinding, int linenum) throws InvalidAPICallException {
        this.methodBinding = methodBinding;
        this._call = getClassName() + "." + getSignature();
        this._throws = new ArrayList<>();
        for (ITypeBinding exception : methodBinding.getExceptionTypes())
            _throws.add(getTypeName(exception, exception.getQualifiedName()));
        this._returns = getTypeName(methodBinding.getReturnType(),
                                            methodBinding.getReturnType().getQualifiedName());
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

    private String getClassName() throws InvalidAPICallException {
        ITypeBinding cls = methodBinding.getDeclaringClass();
        String className = cls.getQualifiedName();
        if (cls.isGenericType())
            className += "<" + String.join(",", Arrays.stream(cls.getTypeParameters()).map(
                    t -> getTypeName(t, t.getName())
            ).collect(Collectors.toList())) + ">";
        if (className.equals(""))
            throw new InvalidAPICallException();
        return className;
    }

    private String getSignature() throws InvalidAPICallException {
        Stream<String> types = Arrays.stream(methodBinding.getParameterTypes()).map(
                t -> getTypeName(t, t.getQualifiedName()));
        if (methodBinding.getName().equals(""))
            throw new InvalidAPICallException();
        return methodBinding.getName() + "(" + String.join(",", types.collect(Collectors.toCollection(ArrayList::new))) + ")";
    }

    private String getTypeName(ITypeBinding binding, String name) {
        return (binding.isTypeVariable()? "Tau_" : "") + name;
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
        String qualifiedName = _call.substring(0, _call.indexOf("("));
        String className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        Type type;
        try {
            type = hasTypeVariable(className)? new Type(constructor.getDeclaringClass())
                    : Type.fromString(className, env.ast());
        } catch (Type.TypeParseException e) {
            System.out.println(className);
            throw new SynthesisException(SynthesisException.TypeParseException);
        }
        type.concretizeType(env);
        creation.setType(type.simpleT(ast, null));

        /* constructor arguments */
        for (int i = 0; i < constructor.getParameterCount(); i++) {
            Parameter param = constructor.getParameters()[i];
            Type argType = type.getConcretization(constructor.getGenericParameterTypes()[i]);
            SearchTarget target = new SearchTarget(argType)
                                        .setParamName(param.getName())
                                        .setAPICallName(constructor.getName())
                                        .setSingleUseVariable(true);
            TypedExpression arg = env.search(target);
            creation.arguments().add(arg.getExpression());
        }

        /* constructor return object */
        TypedExpression ret = env.addVariable(type);
        env.addImport(type.C());

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
        TypedExpression object;
        if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
            Type type = new Type(method.getDeclaringClass());
            type.concretizeType(env);
            object = new TypedExpression(ast.newName(method.getDeclaringClass().getSimpleName()), type);
            env.addImport(method.getDeclaringClass());
        } else {
            String qualifiedName = _call.substring(0, _call.indexOf("("));
            String className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
            Type type;
            try {
                type = hasTypeVariable(className)? new Type(method.getDeclaringClass())
                        : Type.fromString(className, env.ast());
            } catch (Type.TypeParseException e) {
                System.out.println(className);
                throw new SynthesisException(SynthesisException.TypeParseException);
            }
            object = env.search(new SearchTarget(type));
        }
        invocation.setExpression(object.getExpression());

        /* concretize method argument types using the above object and search for them */
        for (int i = 0; i < method.getParameterCount(); i++) {
            Parameter param = method.getParameters()[i];
            Type argType = object.getType().getConcretization(method.getGenericParameterTypes()[i]);
            SearchTarget target = new SearchTarget(argType)
                                        .setParamName(param.getName())
                                        .setAPICallName(method.getName())
                                        .setSingleUseVariable(true);
            TypedExpression arg = env.search(target);
            invocation.arguments().add(arg.getExpression());
        }

        if (method.getReturnType().equals(void.class))
            return invocation;

        /* method return value */
        Type retType = object.getType().getConcretization(method.getGenericReturnType());
        TypedExpression ret = env.addVariable(retType);
        env.addImport(retType.C());

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

    /**
     * Returns a constructor or method based on the _call class variable
     *
     * @return an Executable representing the constructor or method
     * @throws SynthesisException if executable is not found or there is a generic type mismatch during search
     */
    private Executable getConstructorOrMethod() throws SynthesisException {
        /* Step 1: get the type-erased name */
        String qualifiedName = _call.substring(0, _call.indexOf("("));
        String[] args = _call.substring(_call.indexOf("(") + 1, _call.lastIndexOf(")")).split(",");

        String className = qualifiedName.substring(0, qualifiedName.lastIndexOf("."));
        String methodName = qualifiedName.substring(qualifiedName.lastIndexOf(".") + 1);
        String erasedClassName = className.replaceAll("<.*>", "");
        Class cls = Environment.getClass(erasedClassName);
        List<Executable> executables = new ArrayList<>();
        executables.addAll(Arrays.asList(cls.getMethods()));
        executables.addAll(Arrays.asList(cls.getConstructors()));

        /* Step 2: check if there is a direct match of the name (after resolving Taus) */
        List<String> erasedArgs = new ArrayList<>();
        for (String arg : args)
            erasedArgs.add(arg.startsWith("Tau_")? getBound(arg.substring(4), cls).getName() : arg);

        // search methods
        String mName = erasedClassName + "." + methodName + "(" + String.join(",", erasedArgs) + ")";
        String cName = erasedClassName + "(" + String.join(",", erasedArgs) + ")";
        for (Executable e : executables) {
            String eName = getNameAsString(e);
            if ((e instanceof Method && mName.equals(eName)) || (e instanceof Constructor && cName.equals(eName)))
                return e;
        }

        /* Step 3: check if there is a matching name by substituting type variables with bounds */
        for (Executable e : executables) {
            if (e.getParameterCount() != args.length)
                continue;
            List<String> subsArgs = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                java.lang.reflect.Type param = e.getGenericParameterTypes()[i];
                if (arg.startsWith("Tau_"))
                    subsArgs.add(getBound(arg.substring(4), cls).getName());
                else if (param instanceof TypeVariable)
                    subsArgs.add(((Class) (((TypeVariable) param).getBounds()[0])).getName());
                else
                    subsArgs.add(arg);
            }
            String subsMName = erasedClassName + "." + methodName + "(" + String.join(",", subsArgs) + ")";
            String subsCName = erasedClassName + "(" + String.join(",", subsArgs) + ")";
            String eName = getNameAsString(e);
            if ((e instanceof Method && subsMName.equals(eName)) || (e instanceof Constructor && subsCName.equals(eName)))
                return e;
        }

        throw new SynthesisException(SynthesisException.MethodOrConstructorNotFound);
    }

    /**
     * Returns the bounding class of a generic type variable
     *
     * @param typeVarName the name of the generic type variable
     * @param cls the class for which the variable is generic
     * @return the bounding class for the generic variable
     * @throws SynthesisException if there is a type mismatch
     */
    private Class getBound(String typeVarName, Class cls) throws SynthesisException {
        TypeVariable typeVar = null;
        for (TypeVariable t : cls.getTypeParameters())
            if (t.getName().equals(typeVarName)) {
                typeVar = t;
                break;
            }
        if (typeVar == null)
            throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);
        java.lang.reflect.Type bound = typeVar.getBounds()[0]; // first bound is the class
        return (Class) bound;
    }

    /**
     * Returns the name of a given executable from its toString() method
     *
     * @param e the executable
     * @return the name of the executable
     */
    private String getNameAsString(Executable e) {
        for (String s : e.toString().split(" "))
            if (s.contains("("))
                return s.replaceAll("\\$", ".");
        return null;
    }

    private boolean hasTypeVariable(String className) {
        if (className.contains("Tau_"))
            return true;

        // commonly used type variable names in Java API
        Matcher typeVars = Pattern.compile("\\b[EKNTVSU][0-9]?\\b").matcher(className);
        return typeVars.find();
    }
}
