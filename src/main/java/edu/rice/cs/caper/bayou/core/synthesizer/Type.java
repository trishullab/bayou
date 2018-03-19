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

import org.apache.commons.lang3.ClassUtils;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.ParameterizedType;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Type {

    private final Class c;
    private org.eclipse.jdt.core.dom.Type t;
    private Map<String, Type> concretization;
    int refCount;

    static final Map<PrimitiveType.Code,Class> primitiveToClass;
    static {
        Map<PrimitiveType.Code,Class> map = new HashMap<>();
        map.put(PrimitiveType.INT, int.class);
        map.put(PrimitiveType.LONG, long.class);
        map.put(PrimitiveType.DOUBLE, double.class);
        map.put(PrimitiveType.FLOAT, float.class);
        map.put(PrimitiveType.BOOLEAN, boolean.class);
        map.put(PrimitiveType.CHAR, char.class);
        map.put(PrimitiveType.BYTE, byte.class);
        map.put(PrimitiveType.VOID, void.class);
        map.put(PrimitiveType.SHORT, short.class);
        primitiveToClass = Collections.unmodifiableMap(map);
    }

    static final Map<PrimitiveType.Code,String> primitiveToString;
    static {
        Map<PrimitiveType.Code,String> map = new HashMap<>();
        map.put(PrimitiveType.INT, "int");
        map.put(PrimitiveType.LONG, "long");
        map.put(PrimitiveType.DOUBLE, "double");
        map.put(PrimitiveType.FLOAT, "float");
        map.put(PrimitiveType.BOOLEAN, "boolean");
        map.put(PrimitiveType.CHAR, "char");
        map.put(PrimitiveType.BYTE, "byte");
        map.put(PrimitiveType.VOID, "void");
        map.put(PrimitiveType.SHORT, "short");
        primitiveToString = Collections.unmodifiableMap(map);
    }

    static final Map<String,PrimitiveType.Code> stringToPrimitive;
    static {
        Map<String,PrimitiveType.Code> map = new HashMap<>();
        map.put("int", PrimitiveType.INT);
        map.put("long", PrimitiveType.LONG);
        map.put("double", PrimitiveType.DOUBLE);
        map.put("float", PrimitiveType.FLOAT);
        map.put("boolean", PrimitiveType.BOOLEAN);
        map.put("char", PrimitiveType.CHAR);
        map.put("byte", PrimitiveType.BYTE);
        map.put("void", PrimitiveType.VOID);
        map.put("short", PrimitiveType.SHORT);
        stringToPrimitive = Collections.unmodifiableMap(map);
    }

    public Type(org.eclipse.jdt.core.dom.Type t) {
        this.t = t;
        this.c = getClass(t);
        this.refCount = 0;
        autoConcretize();

        if (t.resolveBinding() != null)
            this.t = releaseBinding(t, t.getAST());
    }

    public Type(org.eclipse.jdt.core.dom.Type t, Class c) {
        this.t = t;
        this.c = c;
        this.refCount = 0;
        autoConcretize();

        if (t.resolveBinding() != null)
            this.t = releaseBinding(t, t.getAST());
    }

    public Type(Class c) {
        this.c = c;
        this.t = null;

        // in this case, type has to be concretized manually with an environment before it can be used
    }

    public org.eclipse.jdt.core.dom.Type T() {
        if (t == null)
            throw new SynthesisException(SynthesisException.InvalidKindOfType);
        return t;
    }

    public Class C() {
        return c;
    }

    public static class TypeParseException extends Exception { }

    /**
     * Parses the given string to form a Type object. The string should not contain predicates ($),
     * Tau_s or wildcard types (?)
     *
     * @param typeStr the string to parse as a type
     * @param ast the AST that should own the type
     * @return the Type after parsing
     * @throws TypeParseException if there was a parse error
     */
    public static Type fromString(String typeStr, AST ast) throws TypeParseException {
        if (typeStr.contains("\\$") || typeStr.contains("Tau_") || typeStr.contains("?"))
            throw new SynthesisException(SynthesisException.InvalidKindOfType);

        Matcher sTypePattern = Pattern.compile("\\w+(\\.\\w+)+").matcher(typeStr);
        Matcher aTypePattern = Pattern.compile("([^\\[]*)([\\[\\]]+)").matcher(typeStr);
        Matcher pTypePattern = Pattern.compile("([^<]*)<(.*)>").matcher(typeStr);

        // primitive type
        if (stringToPrimitive.containsKey(typeStr))
            return new Type(ast.newPrimitiveType(stringToPrimitive.get(typeStr)));

        // simple type
        if (sTypePattern.matches())
            return new Type(ast.newSimpleType(ast.newName(typeStr)));

        // array type
        if (aTypePattern.matches()) {
            String base = aTypePattern.group(1);
            String dimensions = aTypePattern.group(2);
            org.eclipse.jdt.core.dom.Type baseType = ast.newSimpleType(ast.newName(base));
            ArrayType arrayType = ast.newArrayType(baseType, dimensions.length() / 2 /* number of []s */);
            return new Type(arrayType);
        }

        // generic type
        if (pTypePattern.matches()) {
            String base = pTypePattern.group(1);
            String args = pTypePattern.group(2);
            org.eclipse.jdt.core.dom.Type baseType = ast.newSimpleType(ast.newName(base));
            Class baseClass = Environment.getClass(base);
            ParameterizedType pType = ast.newParameterizedType(baseType);

            // in principle there should be a (push-down) parser for this, but since we know that the arguments
            // are separated by ","s, a hack is to just try substring'ing each ","  until it parses.
            int currIdx = 0;
            for (int i = 0; i < baseClass.getTypeParameters().length; i++) {
                int nextIdx = currIdx;
                if (i == baseClass.getTypeParameters().length - 1)
                    pType.typeArguments().add(Type.fromString(args.substring(currIdx), ast).T());
                else
                    while (true)
                        try {
                            nextIdx = args.indexOf(",", nextIdx + 1);
                            String paramStr = args.substring(currIdx, nextIdx);
                            Type param = Type.fromString(paramStr, ast);
                            pType.typeArguments().add(param.T());
                            currIdx = nextIdx + 1;
                            break;
                        } catch (TypeParseException e) {
                            // try the next index of ,
                        }
            }
            return new Type(pType);
        }

        throw new TypeParseException();
    }

    public void concretizeType(Environment env) {
        if (t != null)
            return;
        AST ast = env.ast;

        if (c.isPrimitive())
            t = ast.newPrimitiveType(PrimitiveType.toCode(c.getSimpleName()));
        else if (c.isArray()) {
            int dimensions = 0;
            for (Class cls = c; cls.isArray(); cls = cls.getComponentType())
                dimensions++;
            Type componentType = new Type(c.getComponentType());
            componentType.concretizeType(env);
            t = ast.newArrayType(componentType.T(), dimensions);
        }
        else if (c.getTypeParameters().length == 0) // simple type
            t = ast.newSimpleType(ast.newName(c.getCanonicalName()));
        else { // generic type
            concretization = new HashMap<>();
            org.eclipse.jdt.core.dom.Type rawType = ast.newSimpleType(ast.newName(c.getCanonicalName()));
            t = ast.newParameterizedType(rawType);

            // search for a type from the environment and add it as a concretization
            for (TypeVariable tvar : c.getTypeParameters()) {
                String name = tvar.getName();
                Type type = env.searchType();

                ((ParameterizedType) t).typeArguments().add(ASTNode.copySubtree(ast, type.T()));
                concretization.put(name, type);
            }
        }
    }

    public Type getConcretization(java.lang.reflect.Type type) {
        AST ast = t.getAST();

        if (type instanceof java.lang.reflect.ParameterizedType) {
            // substitute generic names with their types (recursively)
            java.lang.reflect.ParameterizedType pType = (java.lang.reflect.ParameterizedType) type;
            java.lang.reflect.Type rawType_ = pType.getRawType();
            org.eclipse.jdt.core.dom.Type rawType = ast.newSimpleType(ast.newName(((Class) rawType_).getCanonicalName()));
            ParameterizedType retType = ast.newParameterizedType(rawType);

            for (java.lang.reflect.Type arg : pType.getActualTypeArguments()) {
                org.eclipse.jdt.core.dom.Type argType = getConcretization(arg).T();
                retType.typeArguments().add(ASTNode.copySubtree(ast, argType));
            }

            return new Type(retType, (Class) rawType_);
        }
        else if (type instanceof TypeVariable) {
            // return the type the generic name was concretized to
            String name = ((TypeVariable) type).getName();

            // FIXME: Add support for wildcard types and concretizing without a base parameterized type (e.g., Collections)
            if (concretization == null)
                throw new SynthesisException(SynthesisException.InvalidKindOfType);

            if (! concretization.containsKey(name))
                throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);
            return concretization.get(name);
        }
        else if (type instanceof Class) {
            Class cls = (Class) type;

            if (cls.isArray()) {
                if (cls.getComponentType().isArray()) // no support for multidim arrays
                    throw new SynthesisException(SynthesisException.InvalidKindOfType);
                Type componentType = getConcretization(cls.getComponentType());
                return new Type(ast.newArrayType(componentType.T(), 1), cls);
            } else if (cls.isPrimitive()) {
                return new Type(ast.newPrimitiveType(PrimitiveType.toCode(cls.getSimpleName())), cls);
            } else {
                // no generics, just return a simple type with the class
                org.eclipse.jdt.core.dom.Type retType = ast.newSimpleType(ast.newName(cls.getCanonicalName()));
                return new Type(retType, cls);
            }
        }
        else throw new SynthesisException(SynthesisException.InvalidKindOfType);
    }

    // same semantics as Class.isAssignableFrom for our type system but with generics
    // NOTE: assumes that the argument is a concretized type
    public boolean isAssignableFrom(Type type) {
        if (! ClassUtils.isAssignable(type.C(), this.C()))
            return false;
        if (t == null || ! t.isParameterizedType()) // this type is not yet concretized or not parametric
            return true;
        if (! type.T().isParameterizedType())
            return false;

        // sanity check
        ParameterizedType pt1 = (ParameterizedType) T();
        ParameterizedType pt2 = (ParameterizedType) type.T();
        int n1 = pt1.typeArguments().size();
        int n2 = pt2.typeArguments().size();
        if (n1 != n2)
            throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);

        for (int i = 0; i < n1; i++) {
            Type t1 = new Type((org.eclipse.jdt.core.dom.Type) pt1.typeArguments().get(i));
            Type t2 = new Type((org.eclipse.jdt.core.dom.Type) pt2.typeArguments().get(i));

            // generic type arguments should always be invariant, not covariant
            // for example, a List<Dog> cannot be a List<Animal> even if Dog extends Animal
            if (! t1.isInvariant(t2))
                return false;
        }
        return true;
    }

    // checks if this type is invariant with the given type
    // NOTE: assumes that the argument is a concretized type
    public boolean isInvariant(Type type) {
        if (this.C() != type.C())
            return false;
        if (t == null || ! t.isParameterizedType()) // this type is not yet concretized or not parametric
            return true;
        if (! type.T().isParameterizedType())
            return false;

        // sanity check
        ParameterizedType pt1 = (ParameterizedType) T();
        ParameterizedType pt2 = (ParameterizedType) type.T();
        int n1 = pt1.typeArguments().size();
        int n2 = pt2.typeArguments().size();
        if (n1 != n2)
            throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);

        for (int i = 0; i < n1; i++) {
            Type t1 = new Type((org.eclipse.jdt.core.dom.Type) pt1.typeArguments().get(i));
            Type t2 = new Type((org.eclipse.jdt.core.dom.Type) pt2.typeArguments().get(i));

            if (! t1.isInvariant(t2))
                return false;
        }
        return true;
    }

    // Class objects are type-erased, so cannot do generics here
    public boolean isAssignableFrom(Class type) {
        if (! ClassUtils.isAssignable(type, this.c))
            return false;
        if (t == null || ! t.isParameterizedType()) // this type is not yet concretized or not parametric
            return true;
        return false; // cannot assign an erased type to a parameterized type
    }

    private void autoConcretize() {
        concretization = new HashMap<>();

        // check sanity of types
        if (! t.isParameterizedType()) {
            if (c.getTypeParameters().length > 0) {
                // commenting this check, as there are cases where synthesis succeeds even if there is a
                // mismatch here (e.g., when there is a variable in scope that has already resolved the type).
                // In other cases when this check would have failed, the failure would now occur elsewhere,
                // typically in the getConcretization(..) case that handles TypeVariable.

                // throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);
            }
            if (t.isArrayType() && !c.isArray())
                throw new SynthesisException(SynthesisException.InvalidKindOfType);
            return;
        }
        ParameterizedType pType = (ParameterizedType) t;
        int n1 = pType.typeArguments().size();
        int n2 = c.getTypeParameters().length;
        if (n1 != n2)
            throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);

        // unify generic names with their actual types
        for (int i = 0; i < n1; i++) {
            String name = c.getTypeParameters()[i].getName();
            Type type = new Type((org.eclipse.jdt.core.dom.Type) pType.typeArguments().get(i));
            concretization.put(name, type);
        }
    }

    Class getClass(org.eclipse.jdt.core.dom.Type type) {
        ITypeBinding binding = type.resolveBinding();
        if (type.isPrimitiveType())
            return primitiveToClass.get(((PrimitiveType) type).getPrimitiveTypeCode());
        else if (type.isSimpleType()) {
            if (binding != null)
                return Environment.getClass(binding.getQualifiedName());
            else {
                String t = (((SimpleType) type).getName()).getFullyQualifiedName();
                return Environment.getClass(t);
            }
        }
        else if (type.isParameterizedType()) {
            if (binding != null) {
                ITypeBinding erased = binding.getErasure();
                return Environment.getClass(erased.getQualifiedName());
            }
            else {
                org.eclipse.jdt.core.dom.Type baseType = ((ParameterizedType) type).getType();
                String t = (((SimpleType) baseType).getName()).getFullyQualifiedName();
                return Environment.getClass(t);
            }
        }
        else if (type.isArrayType()) {
            if (binding != null)
                return Environment.getClass(binding.getErasure().getQualifiedName());
            else {
                org.eclipse.jdt.core.dom.Type elementType = ((ArrayType) type).getElementType();
                StringBuilder name = new StringBuilder();
                if (elementType.isPrimitiveType()) {
                    name.append(primitiveToString.get(((PrimitiveType) elementType).getPrimitiveTypeCode()));
                } else if (elementType.isSimpleType()) {
                    name.append(((SimpleType) elementType).getName().getFullyQualifiedName());
                } else if (elementType.isParameterizedType()) {
                    name.append(((SimpleType) ((ParameterizedType) elementType).getType()).getName().getFullyQualifiedName());
                } else
                    throw new SynthesisException(SynthesisException.InvalidKindOfType);
                for (int i = ((ArrayType) type).getDimensions(); i > 0; i--)
                    name.append("[]"); // add "[]" to denote array type dimension
                return Environment.getClass(name.toString());
            }
        }
        else
            throw new SynthesisException(SynthesisException.InvalidKindOfType);
    }

    // make a DOM type independent of its bindings, because when copying subtrees bindings don't copy over
    org.eclipse.jdt.core.dom.Type releaseBinding(org.eclipse.jdt.core.dom.Type type, AST ast) {
        ITypeBinding binding = type.resolveBinding();
        if (type.isPrimitiveType())
            return ast.newPrimitiveType(((PrimitiveType) type).getPrimitiveTypeCode());
        else if (type.isSimpleType())
            return ast.newSimpleType(ast.newName(binding.getQualifiedName()));
        else if (binding.isParameterizedType()) {
            ITypeBinding erasure = binding.getErasure();
            SimpleType baseType = ast.newSimpleType(ast.newName(erasure.getQualifiedName()));
            ParameterizedType retType = ast.newParameterizedType(baseType);

            for (Object o: ((ParameterizedType) type).typeArguments()) {
                org.eclipse.jdt.core.dom.Type arg = (org.eclipse.jdt.core.dom.Type) o;
                org.eclipse.jdt.core.dom.Type argType = releaseBinding(arg, ast);
                retType.typeArguments().add(argType);
            }

            return retType;
        }
        else if (type.isArrayType()) {
            ArrayType arrayType = (ArrayType) type;
            org.eclipse.jdt.core.dom.Type elementType = releaseBinding(arrayType.getElementType(), ast);
            int dimensions = arrayType.getDimensions();
            return ast.newArrayType(elementType, dimensions);
        }
        else {
            throw new SynthesisException(SynthesisException.InvalidKindOfType);
        }
    }

    /**
     * Returns a simple representation of the current type (i.e., without fully qualified names)
     *
     * @param ast the AST node that should own the simple type
     * @param env if provided (not null), then add imports of classes in generic type
     * @return a DOM type representing the simple type
     */
    public org.eclipse.jdt.core.dom.Type simpleT(AST ast, Environment env) {
        if (t.isPrimitiveType())
            return t;
        if (t.isSimpleType() || t.isQualifiedType()) {
            Name name = t.isSimpleType()? ((SimpleType) t).getName(): ((QualifiedType) t).getName();
            SimpleName simple;
            if (name.isSimpleName())
                simple = ast.newSimpleName(((SimpleName) name).getIdentifier());
            else
                simple = ast.newSimpleName(((QualifiedName) name).getName().getIdentifier());
            return ast.newSimpleType(simple);
        }
        if (t.isParameterizedType()) {
            org.eclipse.jdt.core.dom.Type baseType = ((ParameterizedType) t).getType();
            Name name = baseType.isSimpleType()? ((SimpleType) baseType).getName(): ((QualifiedType) baseType).getName();
            SimpleName simple;
            if (name.isSimpleName())
                simple = ast.newSimpleName(((SimpleName) name).getIdentifier());
            else
                simple = ast.newSimpleName(((QualifiedName) name).getName().getIdentifier());
            ParameterizedType pType = ast.newParameterizedType(ast.newSimpleType(simple));
            for (Object o : ((ParameterizedType) t).typeArguments()) {
                Type p = new Type((org.eclipse.jdt.core.dom.Type) o);
                if (env != null)
                    env.addImport(p.C());
                pType.typeArguments().add(p.simpleT(ast, env));
            }
            return pType;
        }
        if (t.isArrayType()) {
            org.eclipse.jdt.core.dom.Type elementType = ((ArrayType) t).getElementType();
            org.eclipse.jdt.core.dom.Type simpleElementType = new Type(elementType).simpleT(ast, env);
            return ast.newArrayType((org.eclipse.jdt.core.dom.Type) ASTNode.copySubtree(ast, simpleElementType),
                    ((ArrayType) t).getDimensions());
        }
        throw new SynthesisException(SynthesisException.InvalidKindOfType);
    }

    public boolean isConcretized() {
        return t != null;
    }

    public void addRefCount() {
        refCount += 1;
    }

    public String toString() {
        return (t != null? t : c).toString();
    }

    /**
     * Checks for equality of two types by comparing the class AND the DOM type
     * @param o the object to compare with
     * @return whether they are equal
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof Type))
            return false;
        Type type = (Type) o;
        return C().equals(type.C()) && (T() == null? type.T() == null : T().subtreeMatch(new ASTMatcher(), type.T()));
    }

    /**
     * Returns the hash code based only on class.
     * This is fine since equals(t1, t2) => hashCode(t1) == hashCode(t2), but not the other way around.
     * @return this type's hash code
     */
    @Override
    public int hashCode() {
        return 7*C().hashCode();
    }
}
