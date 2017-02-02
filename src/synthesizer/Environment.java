package synthesizer;

import org.eclipse.jdt.core.dom.AST;

import java.util.*;

public class Environment {

    List<Variable> scope; // unmutable
    List<Variable> mu_scope; // mutable
    Map<Variable,Integer> usageCounts;
    Map<String,Integer> prettyNameCounts;

    Set<Class> imports;

    Map<Class,Integer> exceptions;

    final AST ast;

    public AST ast() {
        return ast;
    }

    public Environment(AST ast, List<Variable> scope) {
        this.ast = ast;
        this.scope = Collections.unmodifiableList(scope);
        mu_scope = new ArrayList<>();
        usageCounts = new HashMap<>();
        prettyNameCounts = new HashMap<>();
        imports = new HashSet<>();
        exceptions = new HashMap<>();

        for (Variable v : scope)
            usageCounts.put(v, 1);
    }

    private Variable search(Class type) {
        int minUsageCount = Integer.MAX_VALUE;
        Variable var = null;
        for (Variable v : scope)
            if (type.isAssignableFrom(v.getType()) && usageCounts.get(v) <= minUsageCount)
                var = v;
        for (Variable v : mu_scope)
            if (type.isAssignableFrom(v.getType()) && usageCounts.get(v) <= minUsageCount)
                var = v;
        return var;
    }

    public Variable searchOrAddVariable(Class type, boolean search) {
        Variable var;
        if (search && (var = search(type)) != null) {
            usageCounts.put(var, usageCounts.get(var) + 1);
            return var;
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
        var = new Variable(name, type);
        usageCounts.put(var, 1);
        mu_scope.add(var);

        /* add type to imports */
        imports.add(type);

        return var;
    }

    public void recordExceptionThrown(Class c) {
        if (!exceptions.containsKey(c))
            exceptions.put(c, 0);
        exceptions.put(c, exceptions.get(c)+1);
    }

    public void recordExceptionCaught(Class c) {
        exceptions.put(c, exceptions.get(c)-1);
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
}
