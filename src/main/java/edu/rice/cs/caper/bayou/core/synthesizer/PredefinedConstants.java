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
import org.eclipse.jdt.core.dom.FieldAccess;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;

/**
 * A class that handles predefined constants in the search space of expressions
 */
public class PredefinedConstants {

    /**
     * Set of classes (types) that have predefined constants
     */
    private Set<Class> predefined;

    /**
     * Initializes this predefined constants handler. Add new predefined constant types here.
     */
    public PredefinedConstants() {
        predefined = new HashSet<>();

        predefined.add(InputStream.class);
        predefined.add(PrintStream.class);
    }

    /**
     * Checks if a given class (type) has a predefined constant
     *
     * @param c class to check for
     * @return true or false indicating if a predefined constant exists for the given class (type)
     */
    public boolean hasPredefinedConstant(Class c) {
        return predefined.contains(c);
    }

    /**
     * Returns a typed expression representing a predefined constant for the given class (type), owned by the given AST
     *
     * @param c   class to get the predefined constant for
     * @param ast AST that owns the created expression
     * @return TypedExpression representing predefined constant (if any), or null
     */
    public TypedExpression getTypedExpression(Class c, AST ast) {
        if (c == InputStream.class)
            return getInputStreamExpr(ast);
        if (c == PrintStream.class)
            return getPrintStreamExpr(ast);
        return null;
    }

    /**
     * Returns a predefined constant for the InputStream class: "System.in"
     *
     * @param ast AST that owns the returned expression
     * @return TypedExpression for "System.in"
     */
    private TypedExpression getInputStreamExpr(AST ast) {
        FieldAccess access = ast.newFieldAccess();
        access.setExpression(ast.newSimpleName("System"));
        access.setName(ast.newSimpleName("in"));

        org.eclipse.jdt.core.dom.Type type = ast.newSimpleType(ast.newName("java.io.InputStream"));
        TypedExpression tExpr = new TypedExpression(access, new Type(type, InputStream.class));
        return tExpr;
    }

    /**
     * Returns a predefined constant for the PrintStream class: "System.out"
     *
     * @param ast AST that owns the returned expression
     * @return TypedExpression for "System.out"
     */
    private TypedExpression getPrintStreamExpr(AST ast) {
        FieldAccess access = ast.newFieldAccess();
        access.setExpression(ast.newSimpleName("System"));
        access.setName(ast.newSimpleName("out"));

        org.eclipse.jdt.core.dom.Type type = ast.newSimpleType(ast.newName("java.io.PrintStream"));
        TypedExpression tExpr = new TypedExpression(access, new Type(type, PrintStream.class));
        return tExpr;
    }
}
