package edu.rice.cs.caper.lib.bayou.dsl;

import edu.rice.cs.caper.lib.bayou.synthesizer.Environment;
import org.eclipse.jdt.core.dom.*;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DSubTree extends DASTNode {

    final String node = "DSubTree";
    final List<DASTNode> _nodes;

    public DSubTree() {
        _nodes = new ArrayList<>();
    }

    public DSubTree(List<DASTNode> _nodes) {
        this._nodes = _nodes;
    }

    public void addNode(DASTNode node) {
        _nodes.add(node);
    }

    public void addNodes(List<DASTNode> otherNodes) {
        _nodes.addAll(otherNodes);
    }

    public boolean isValid() {
        return !_nodes.isEmpty();
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max) throws TooManySequencesException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (DASTNode node : _nodes)
            node.updateSequences(soFar, max);
    }

    @Override
    public Set<String> keywords() {
        Set<String> kw = new HashSet<>();
        for (DASTNode node : _nodes)
            kw.addAll(node.keywords());
        return kw;
    }

    public List<DAPICall> getNodesAsCalls() {
        List<DAPICall> calls = new ArrayList<>();
        for (DASTNode node : _nodes) {
            assert node instanceof DAPICall : "invalid branch condition";
            calls.add((DAPICall) node);
        }
        return calls;
    }

    public List<DASTNode> getNodes() {
        return _nodes;
    }

    @Override
    public int numStatements() {
        int num = 0;
        for (DASTNode node : _nodes)
            num += node.numStatements();
        return num;
    }

    @Override
    public int numLoops() {
        int num = 0;
        for (DASTNode node : _nodes)
            num += node.numLoops();
        return num;
    }

    @Override
    public int numBranches() {
        int num = 0;
        for (DASTNode node : _nodes)
            num += node.numBranches();
        return num;
    }

    @Override
    public int numExcepts() {
        int num = 0;
        for (DASTNode node : _nodes)
            num += node.numExcepts();
        return num;
    }

    @Override
    public Set<DAPICall> bagOfAPICalls() {
        Set<DAPICall> bag = new HashSet<>();
        for (DASTNode node : _nodes)
            bag.addAll(node.bagOfAPICalls());
        return bag;
    }

    @Override
    public Set<Class> exceptionsThrown() {
        Set<Class> ex = new HashSet<>();
        for (DASTNode n : _nodes)
            ex.addAll(n.exceptionsThrown());
        return ex;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DSubTree))
            return false;
        DSubTree tree = (DSubTree) o;
        return _nodes.equals(tree.getNodes());
    }

    @Override
    public int hashCode() {
        return _nodes.hashCode();
    }

    @Override
    public String toString() {
        List<String> _nodesStr = _nodes.stream().map(node -> node.toString()).collect(Collectors.toList());
        return String.join("\n", _nodesStr);
    }



    @Override
    public Block synthesize(Environment env) {
        AST ast = env.ast();
        Block block = ast.newBlock();

        for (DASTNode dNode : _nodes) {
            ASTNode aNode = dNode.synthesize(env);
            if (aNode instanceof Statement)
                block.statements().add(aNode);
            else
                block.statements().add(ast.newExpressionStatement((Expression) aNode));
        }

        return block;
    }
}
