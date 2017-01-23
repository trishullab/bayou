package dsl;

import java.util.ArrayList;
import java.util.List;

public class DBranch extends DASTNode {

    final String node = "DBranch";
    final List<DAPICall> _cond;
    final List<DASTNode> _then;
    final List<DASTNode> _else;

    public DBranch(List<DAPICall> _cond, List<DASTNode> _then, List<DASTNode> _else) {
        this._cond = _cond;
        this._then = _then;
        this._else = _else;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DAPICall call : _cond)
            call.updateSequences(soFar);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.calls));
        for (DASTNode t : _then)
            t.updateSequences(soFar);
        for (DASTNode e : _else)
            e.updateSequences(copy);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DBranch))
            return false;
        DBranch branch = (DBranch) o;
        return _cond.equals(branch._cond) && _then.equals(branch._then) && _else.equals(branch._else);
    }

    @Override
    public int hashCode() {
        return 7* _cond.hashCode() + 17* _then.hashCode() + 31* _else.hashCode();
    }
}
