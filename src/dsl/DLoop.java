package dsl;

import dom_driver.Visitor;

import java.util.List;

public class DLoop extends DASTNode {

    final String node = "DLoop";
    final List<DAPICall> _cond;
    final List<DASTNode> _body;

    public DLoop(List<DAPICall> cond, List<DASTNode> _body) {
        this._cond = cond;
        this._body = _body;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DAPICall call : _cond)
            call.updateSequences(soFar);

        for (int i = 0; i < Visitor.V().options.NUM_UNROLLS; i++) {
            for (DASTNode node : _body)
                node.updateSequences(soFar);
            for (DAPICall call : _cond)
                call.updateSequences(soFar);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DLoop))
            return false;
        DLoop loop = (DLoop) o;
        return _cond.equals(loop._cond) && _body.equals(loop._body);
    }

    @Override
    public int hashCode() {
        return 7* _cond.hashCode() + 17* _body.hashCode();
    }
}
