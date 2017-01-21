package dsl;

import java.util.ArrayList;
import java.util.List;

public class DSubTree extends DASTNode {

    final String node = "DSubTree";
    final List<DASTNode> nodes;

    public DSubTree() {
        nodes = new ArrayList<>();
    }

    public DSubTree(List<DASTNode> nodes) {
        this.nodes = nodes;
    }

    public void addNode(DASTNode node) {
        nodes.add(node);
    }

    public void addNodes(List<DASTNode> otherNodes) {
        nodes.addAll(otherNodes);
    }

    public boolean isValid() {
        return !nodes.isEmpty();
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DASTNode node : nodes)
            node.updateSequences(soFar);
    }

    public List<DAPICall> getNodesAsCalls() {
        List<DAPICall> calls = new ArrayList<>();
        for (DASTNode node : nodes) {
            assert node instanceof DAPICall : "invalid branch condition";
            calls.add((DAPICall) node);
        }
        return calls;
    }

    public List<DASTNode> getNodes() {
        return nodes;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DSubTree))
            return false;
        DSubTree tree = (DSubTree) o;
        return nodes.equals(tree.getNodes());
    }

    @Override
    public int hashCode() {
        return nodes.hashCode();
    }
}
