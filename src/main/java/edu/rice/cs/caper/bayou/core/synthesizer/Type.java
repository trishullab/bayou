package edu.rice.cs.caper.bayou.core.synthesizer;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.ParameterizedType;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

public class Type {

    /* TODO: add support for arrays (search for isArray) */

    private final Class c;
    private org.eclipse.jdt.core.dom.Type t;
    private Map<String, Type> concretization;
    int refCount;

    public Type(org.eclipse.jdt.core.dom.Type t) {
        this.t = t;
        this.c = getClass(t);

        if (this.c.isArray())
            throw new SynthesisException(SynthesisException.InvalidKindOfType);

        this.refCount = 0;
        autoConcretize();
    }

    public Type(org.eclipse.jdt.core.dom.Type t, Class c) {
        this.t = t;
        this.c = c;

        if (this.c.isArray())
            throw new SynthesisException(SynthesisException.InvalidKindOfType);

        this.refCount = 0;
        autoConcretize();
    }

    public Type(Class c) {
        this.c = c;
        this.t = null;

        if (this.c.isArray())
            throw new SynthesisException(SynthesisException.InvalidKindOfType);

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

        if (c.isArray())
            throw new SynthesisException(SynthesisException.InvalidKindOfType);

        if (c.isPrimitive())
            t = ast.newPrimitiveType(PrimitiveType.toCode(c.getSimpleName()));
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
            if (! concretization.containsKey(name))
                throw new SynthesisException(SynthesisException.GenericTypeVariableMismatch);
            return concretization.get(name);
        }
        else if (type instanceof Class) {
            Class cls = (Class) type;

            if (cls.isArray())
                throw new SynthesisException(SynthesisException.InvalidKindOfType);

            if (cls.isPrimitive())
                return new Type(ast.newPrimitiveType(PrimitiveType.toCode(cls.getSimpleName())), cls);
            else {
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
        if (! this.C().isAssignableFrom(type.C()))
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
        if (! this.C().isAssignableFrom(type))
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
            return Visitor.primitiveToClass.get(((PrimitiveType) type).getPrimitiveTypeCode());
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
        else
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
