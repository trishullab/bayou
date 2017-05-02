package edu.rice.bayou.synthesizer;

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
        return searchOrAddVariable(type, false);
    }

    public Expression searchOrAddVariable(Class type, boolean search) {
        Expression expr;
        Enumerator enumerator = new Enumerator(ast, this);
        if (search && (expr = enumerator.search(type)) != null) {
            return expr;
        }

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

    public static Class getClass(String name) {
        Class cls = null;
        try {
            cls = Class.forName(name, false, Synthesizer.classLoader);
        } catch (ClassNotFoundException _e) {
            /* check if it's an inner class (limitation: only one level down) */
            int dot = name.lastIndexOf('.');
            String innerClassName = new StringBuilder(name).replace(dot, dot+1, "$").toString();
            try {
                cls = Class.forName(innerClassName, false, Synthesizer.classLoader);
            } catch (ClassNotFoundException e) {
                System.err.println("Class " + name + " could not be loaded!");
                e.printStackTrace();
                System.exit(1);
            }
        }
        return cls;
    }

    public void addImport(Class c) {
        imports.add(c);
    }
}
