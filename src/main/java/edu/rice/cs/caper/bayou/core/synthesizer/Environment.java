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

    private final Stack<Scope> scopes;
    Set<Class> imports;
    final AST ast;
    final Synthesizer.Mode mode;

    public AST ast() {
        return ast;
    }

    public Environment(AST ast, List<Variable> variables, Synthesizer.Mode mode) {
        this.ast = ast;
        this.scopes = new Stack<>();
        this.scopes.push(new Scope(variables));
        this.mode = mode;
        imports = new HashSet<>();
    }

    public TypedExpression addVariable(Type type) {
        return addVariable(type, true, false);
    }

    public TypedExpression addVariable(Type type, boolean join, boolean defaultInit) {
        /* add variable to scope */
        Variable var = scopes.peek().addVariable(type);
        var.setJoin(join);
        var.setDefaultInit(defaultInit);

        /* add type to imports */
        imports.add(type.C());

        return new TypedExpression(ast.newSimpleName(var.getName()), type);
    }

    public Type searchType() {
        Enumerator enumerator = new Enumerator(ast, this, mode);
        return enumerator.searchType();
    }

    public TypedExpression search(Type type) throws SynthesisException {
        Enumerator enumerator = new Enumerator(ast, this, mode);
        TypedExpression tExpr = enumerator.search(type);
        if (tExpr == null)
            throw new SynthesisException(SynthesisException.TypeNotFoundDuringSearch);
        return tExpr;
    }

    public Scope getScope() {
        return scopes.peek();
    }

    public void pushScope() {
        Scope newScope = new Scope(scopes.peek());
        scopes.push(newScope);
    }

    public Scope popScope() {
        return scopes.pop();
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

}
