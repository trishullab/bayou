package dsl;

public abstract class Handler {
    Visitor visitor;
    protected Handler(Visitor visitor) {
        this.visitor = visitor;
    }

    protected Handler() {}

    abstract DASTNode handle();
}
