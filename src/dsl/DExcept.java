package dsl;

import java.util.ArrayList;
import java.util.List;

public class DExcept extends DASTNode {

    final String node = "DExcept";
    public List<DASTNode> _try;
    public List<DASTNode> _catch;

    public DExcept(List<DASTNode> _try, List<DASTNode> _catch) {
        this._try = _try;
        this._catch = _catch;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DASTNode node : _try)
            node.updateSequences(soFar);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.calls));
        for (DASTNode e : _catch)
            e.updateSequences(copy);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DExcept))
            return false;
        DExcept other = (DExcept) o;
        return _try.equals(other._try) && _catch.equals(other._catch);
    }

    @Override
    public int hashCode() {
        return 7* _try.hashCode() + 17* _catch.hashCode();
    }

    @Override
    public String toString() {
        return "try {\n" + _try + "\n} catch {\n" + _catch + "\n}";
    }
}
