package synthesizer;

public class Variable {

    final String name;
    final Class type;
    int refCount;

    Variable(String name, Class type) {
        this.name = name;
        this.type = type;
        refCount = 0;
    }

    public String getName() {
        return name;
    }

    public Class getType() {
        return type;
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
