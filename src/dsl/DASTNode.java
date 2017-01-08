package dsl;

import java.util.List;

public abstract class DASTNode {
    abstract void updateSequences(List<Sequence> soFar);
    abstract String sketch();
    protected String HOLE() { return "<hole>"; }
}
