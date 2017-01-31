package dsl;

import synthesizer.Synthesizable;

import java.util.List;

public abstract class DASTNode implements Synthesizable {
    public abstract void updateSequences(List<Sequence> soFar);

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();
}
