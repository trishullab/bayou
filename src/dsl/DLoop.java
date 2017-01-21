package dsl;

import dom_driver.Visitor;

import java.util.List;

public class DLoop extends DASTNode {

    final String node = "DLoop";
    final List<DAPICall> cond;
    final List<DASTNode> body;

    public DLoop(List<DAPICall> cond, List<DASTNode> body) {
        this.cond = cond;
        this.body = body;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DAPICall call : cond)
            call.updateSequences(soFar);

        for (int i = 0; i < Visitor.V().options.NUM_UNROLLS; i++) {
            for (DASTNode node : body)
                node.updateSequences(soFar);
            for (DAPICall call : cond)
                call.updateSequences(soFar);
        }
    }

    public List<DAPICall> getCond() {
        return cond;
    }

    public List<DASTNode> getBody() {
        return body;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DLoop))
            return false;
        DLoop loop = (DLoop) o;
        return cond.equals(loop.cond) && body.equals(loop.body);
    }

    @Override
    public int hashCode() {
        return 7*cond.hashCode() + 17*body.hashCode();
    }
}
