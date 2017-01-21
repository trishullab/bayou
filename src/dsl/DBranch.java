package dsl;

import java.util.ArrayList;
import java.util.List;

public class DBranch extends DASTNode {

    final String node = "DBranch";
    final List<DAPICall> cond;
    final List<DASTNode> then;
    final List<DASTNode> else_;

    public DBranch(List<DAPICall> cond, List<DASTNode> then, List<DASTNode> else_) {
        this.cond = cond;
        this.then = then;
        this.else_ = else_;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DAPICall call : cond)
            call.updateSequences(soFar);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.calls));
        for (DASTNode t : then)
            t.updateSequences(soFar);
        for (DASTNode e : else_)
            e.updateSequences(copy);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
    }

    public List<DAPICall> getCond() {
        return cond;
    }

    public List<DASTNode> getThen() {
        return then;
    }

    public List<DASTNode> getElse_() {
        return else_;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DBranch))
            return false;
        DBranch branch = (DBranch) o;
        return cond.equals(branch.cond) && then.equals(branch.then) && else_.equals(branch.else_);
    }

    @Override
    public int hashCode() {
        return 7*cond.hashCode() + 17*then.hashCode() + 31*else_.hashCode();
    }
}
