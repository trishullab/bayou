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

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.Expression;

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

    public Expression addVariable(Class type) {
        try {
            return searchOrAddVariable(type, false);
        } catch (SynthesisException e) {
            throw new Error("SynthesisException was thrown in addVariable()! This shouldn't occur.");
        }
    }

    public Expression searchOrAddVariable(Class type, boolean search) throws SynthesisException {
        Expression expr;
        Enumerator enumerator = new Enumerator(ast, this);
        if (search)
            if ((expr = enumerator.search(type)) != null)
                return expr;
            else throw new SynthesisException("Could not find variable of type " + type.getName());

        /* construct a nice name for the variable */
        String name = "";
        if (type.isPrimitive())
            name = type.getName().substring(0,1);
        else
            for (Character c : type.getName().toCharArray())
                if (Character.isUpperCase(c))
                    name += Character.toLowerCase(c);
        if (prettyNameCounts.containsKey(name)) {
            prettyNameCounts.put(name, prettyNameCounts.get(name)+1);
            name += prettyNameCounts.get(name);
        }
        else
            prettyNameCounts.put(name, 0);

        /* add variable to scope */
        Variable var = new Variable(name, type);
        mu_scope.add(var);

        /* add type to imports */
        imports.add(type);

        return ast.newSimpleName(var.getName());
    }

    public Variable addScopedVariable(String name, Class type) {
        Variable var = new Variable(name, type);
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
     * @throws ClassNotFoundException if no classes with the name search name or its variations are found.
     */
    public static Class getClass(String name) throws ClassNotFoundException
    {
        return getClassHelp(name, name);
    }

    /**
     * Attempts to find the Class representation of the given fully qualified <code>searchName</code> from
     * <code>Synthesizer.classLoader</code>.
     *
     * If no such class is found and the given searchName contains the character '.', a new search name will
     * be generated replacing the final '.' with a '$' and this method recurses.  As such, if the given searchName
     * is
     *     foo.bar.baz
     *
     * then this method will effectively search for the following classes in order until one (or none) is found:
     *
     *     foo.bar.baz
     *     foo.bar$baz
     *     foo$bar$baz
     *     << throws ClassNotFoundException of originalName >>
     *
     * @param searchName the fully qualified class name to search for
     * @param originalName  if no variations of searchName are found, the name reported in the exception message
     *                      of ClassNotFoundException
     * @return the Class representation of searchName (or an attempted alternate) if found
     * @throws ClassNotFoundException if no classes with the name search name or its variations are found.
     */
    private static Class getClassHelp(String searchName, String originalName) throws ClassNotFoundException
    {
        try {

            return Class.forName(searchName, false, SynthesizerDefault.classLoader);

        } catch (ClassNotFoundException e) {

            int lastDotIndex = searchName.lastIndexOf('.');
            if(lastDotIndex == -1) {
                throw new ClassNotFoundException(originalName);
            }

            String possibleInnerClassName =
                    new StringBuilder(searchName).replace(lastDotIndex, lastDotIndex+1, "$").toString();

            return getClassHelp(possibleInnerClassName, originalName);

        }
    }

    public void addImport(Class c) {
        imports.add(c);
    }
}
