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
            if (c.getTypeParameters().length > 0)
                throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);
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

    public org.eclipse.jdt.core.dom.Type simpleT(AST ast) {
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
            return ast.newSimpleType(simple);
        }
        if (t.isArrayType()) {
            org.eclipse.jdt.core.dom.Type elementType = ((ArrayType) t).getElementType();
            org.eclipse.jdt.core.dom.Type simpleElementType = new Type(elementType).simpleT(ast);
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
}
