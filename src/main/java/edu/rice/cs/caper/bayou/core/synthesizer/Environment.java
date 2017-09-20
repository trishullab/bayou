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

import java.util.*;

public class Environment {

    List<Variable> scope; // unmutable
    List<Variable> mu_scope; // mutable
    Map<String,Integer> prettyNameCounts;

    Set<Class> imports;
    final AST ast;

    public AST ast() {
        return ast;
    }

    public Environment(AST ast, List<Variable> scope) {
        this.ast = ast;
        this.scope = Collections.unmodifiableList(scope);
        mu_scope = new ArrayList<>();
        prettyNameCounts = new HashMap<>();
        imports = new HashSet<>();
    }

    public TypedExpression addVariable(Type type) {
        /* construct a nice name for the variable */
        String name = getPrettyName(type);

        /* add variable to scope */
        Variable var = new Variable(name, type);
        mu_scope.add(var);

        /* add type to imports */
        imports.add(type.C());

        return new TypedExpression(ast.newSimpleName(var.getName()), type);
    }

    public Type searchType() {
        Enumerator enumerator = new Enumerator(ast, this);
        return enumerator.searchType();
    }

    public TypedExpression search(Type type) throws SynthesisException {
        Enumerator enumerator = new Enumerator(ast, this);
        TypedExpression tExpr = enumerator.search(type);
        if (tExpr == null)
            throw new SynthesisException(SynthesisException.TypeNotFoundDuringSearch);
        return tExpr;
    }

    public Variable addScopedVariable(String name, Class cls) {
        Type t;
        if (cls.isPrimitive())
            t = new Type(ast.newPrimitiveType(PrimitiveType.toCode(cls.getName())), cls);
        else
            t = new Type(ast.newSimpleType(ast.newSimpleName(cls.getSimpleName())), cls);
        Variable var = new Variable(name, t);
        mu_scope.add(var);
        return var;
    }

    public void removeScopedVariable(Variable v) {
        mu_scope.remove(v);
    }

    /**
     * Attempts to find the Class representation of the given fully qualified <code>name</code> from
     * <code>Synthesizer.classLoader</code>.
     *
     * If no such class is found and the given name contains the character '.', a new search name will
     * be generated replacing the final '.' with a '$' and the search will continue in an iterative fashion.
     *
     * For example, if the given name is
     *
     *     foo.bar.baz
     *
     * then this method will effectively search for the following classes in order until one (or none) is found:
     *
     *     foo.bar.baz
     *     foo.bar$baz
     *     foo$bar$baz
     *     << throws ClassNotFoundException  >>
     *
     * @param name the fully qualified class name to search for
     * @return the Class representation of name (or an attempted alternate) if found
     */
    public static Class getClass(String name) {
        try {
            return Class.forName(name, false, Synthesizer.classLoader);
        } catch (ClassNotFoundException e) {
            int lastDotIndex = name.lastIndexOf('.');
            if (lastDotIndex == -1)
                throw new SynthesisException(SynthesisException.ClassNotFoundInLoader);
            String possibleInnerClassName =
                    new StringBuilder(name).replace(lastDotIndex, lastDotIndex+1, "$").toString();
            return getClass(possibleInnerClassName);
        }
    }

    public void addImport(Class c) {
        imports.add(c);
    }

    String getPrettyName(Type type) {
        String name;
        if (type.C().isPrimitive())
            name = type.C().getSimpleName().substring(0, 1);
        else {
            name = "";
            for (Character c : type.C().getName().toCharArray())
                if (Character.isUpperCase(c))
                    name += Character.toLowerCase(c);
        }

        if (prettyNameCounts.containsKey(name)) {
            prettyNameCounts.put(name, prettyNameCounts.get(name)+1);
            name += prettyNameCounts.get(name);
        }
        else
            prettyNameCounts.put(name, 0);

        return name;
    }
}
