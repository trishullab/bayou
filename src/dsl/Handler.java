package dsl;

import java.util.List;

public abstract class Handler {
    protected Visitor visitor;
    protected Handler(Visitor visitor) {
        this.visitor = visitor;
    }

    protected Handler() {}

    abstract DASTNode handle();
    abstract void updateSequences(List<Sequence> soFar);
}
