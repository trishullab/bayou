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

public class Variable {

    final String name;
    final Type type;
    final Class clazz;
    int refCount;

    Variable(String name, Type type) {
        this.name = name;
        this.type = type;
        refCount = 0;

        ITypeBinding binding = type.resolveBinding();
        if (type.isPrimitiveType())
            this.clazz = Visitor.primitiveToClass.get(((PrimitiveType) type).getPrimitiveTypeCode());
        else if (type.isSimpleType()) {
            if (binding != null)
                try {
                    this.clazz = Environment.getClass(binding.getQualifiedName());
                } catch (ClassNotFoundException e) {
                    throw new SynthesisException(SynthesisException.ClassNotFoundInLoader);
                }
            else
                try {
                    String t = (((SimpleType) type).getName()).getFullyQualifiedName();
                    this.clazz = Environment.getClass(t);
                } catch (ClassNotFoundException e) {
                    throw new SynthesisException(SynthesisException.ClassNotFoundInLoader);
                }
        }
        else if (type.isParameterizedType()) {
            if (binding != null)
                try {
                    ITypeBinding erased = binding.getErasure();
                    this.clazz = Environment.getClass(erased.getQualifiedName());
                } catch (ClassNotFoundException e) {
                    throw new SynthesisException(SynthesisException.ClassNotFoundInLoader);
                }
            else
                try {
                    Type baseType = ((ParameterizedType) type).getType();
                    String t = (((SimpleType) baseType).getName()).getFullyQualifiedName();
                    this.clazz = Environment.getClass(t);
                } catch (ClassNotFoundException e) {
                    throw new SynthesisException(SynthesisException.ClassNotFoundInLoader);
                }
        }
        else // TODO: support generic types (isParameterizedType())
            throw new SynthesisException(SynthesisException.InvalidKindOfType);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public Class getTypeAsClass() {
        return clazz;
    }

    public void addRefCount() {
        refCount += 1;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof Variable))
            return false;
        Variable v = (Variable) o;
        return v.name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
