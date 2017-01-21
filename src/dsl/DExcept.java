package dsl;

import java.util.ArrayList;
import java.util.List;

public class DExcept extends DASTNode {

    final String node = "DExcept";
    public List<DASTNode> try_;
    public List<DASTNode> except;

    public DExcept(List<DASTNode> try_, List<DASTNode> except) {
        this.try_ = try_;
        this.except = except;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DASTNode node : try_)
            node.updateSequences(soFar);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.calls));
        for (DASTNode e : except)
            e.updateSequences(copy);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
    }

    public List<DASTNode> getTry_() {
        return try_;
    }

    public List<DASTNode> getExcept() {
        return except;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DExcept))
            return false;
        DExcept other = (DExcept) o;
        return try_.equals(other.try_) && except.equals(other.except);
    }

    @Override
    public int hashCode() {
        return 7*try_.hashCode() + 17*except.hashCode();
    }
}
