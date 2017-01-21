package dsl;

import java.util.List;

public abstract class DASTNode {
    public abstract void updateSequences(List<Sequence> soFar);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
