package edu.rice.bayou.dsl;

import edu.rice.bayou.dom_driver.Visitor;
import org.eclipse.jdt.core.dom.*;
import edu.rice.bayou.synthesizer.Environment;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DLoop extends DASTNode {

    final String node = "DLoop";
    final List<DAPICall> _cond;
    final List<DASTNode> _body;

    public DLoop(List<DAPICall> cond, List<DASTNode> _body) {
        this._cond = cond;
        this._body = _body;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length) throws TooManySequencesException, TooLongSequenceException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (DAPICall call : _cond)
            call.updateSequences(soFar, max, max_length);

        int num_unrolls = Visitor.V() == null? 1: Visitor.V().options.NUM_UNROLLS;
        for (int i = 0; i < num_unrolls; i++) {
            for (DASTNode node : _body)
                node.updateSequences(soFar, max, max_length);
            for (DAPICall call : _cond)
                call.updateSequences(soFar, max, max_length);
        }
    }

    @Override
    public Set<String> keywords() {
        Set<String> kw = new HashSet<>();
        for (DAPICall c : _cond)
            kw.addAll(c.keywords());
        for (DASTNode b : _body)
            kw.addAll(b.keywords());
        return kw;
    }

    @Override
    public int numStatements() {
        int num = _cond.size();
        for (DASTNode b : _body)
            num += b.numStatements();
        return num;
    }

    @Override
    public int numLoops() {
        int num = 1; // this loop
        for (DASTNode b : _body)
            num += b.numLoops();
        return num;
    }

    @Override
    public int numBranches() {
        int num = 0;
        for (DASTNode b : _body)
            num += b.numBranches();
        return num;
    }

    @Override
    public int numExcepts() {
        int num = 0;
        for (DASTNode b : _body)
            num += b.numExcepts();
        return num;
    }

    @Override
    public Set<DAPICall> bagOfAPICalls() {
        Set<DAPICall> bag = new HashSet<>();
        bag.addAll(_cond);
        for (DASTNode b : _body)
            bag.addAll(b.bagOfAPICalls());
        return bag;
    }

    @Override
    public Set<Class> exceptionsThrown() {
        Set<Class> ex = new HashSet<>();
        for (DAPICall c : _cond)
            ex.addAll(c.exceptionsThrown());
        for (DASTNode b : _body)
            ex.addAll(b.exceptionsThrown());
        return ex;
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

    @Override
    public String toString() {
        return "while (\n" + _cond + "\n) {\n" + _body + "\n}";
    }



    @Override
    public WhileStatement synthesize(Environment env) {
        AST ast = env.ast();
        WhileStatement statement = ast.newWhileStatement();

        /* synthesize the condition */
        List<Expression> clauses = new ArrayList<>();
        for (DAPICall call : _cond) {
            /* this cast is safe (unless NN has gone crazy) because a call that returns void cannot be in condition */
            Assignment assignment = (Assignment) call.synthesize(env);
            if (call.method == null || (!call.method.getReturnType().equals(Boolean.class) &&
                    !call.method.getReturnType().equals(boolean.class))) {
                ParenthesizedExpression pAssignment = ast.newParenthesizedExpression();
                pAssignment.setExpression(assignment);
                InfixExpression notEqualsNull = ast.newInfixExpression();
                notEqualsNull.setLeftOperand(pAssignment);
                notEqualsNull.setOperator(InfixExpression.Operator.NOT_EQUALS);
                notEqualsNull.setRightOperand(ast.newNullLiteral());

                clauses.add(notEqualsNull);
            }
            else
                clauses.add(assignment);
        }
        switch (clauses.size()) {
            case 0:
                Expression var = env.searchOrAddVariable(boolean.class, true);
                statement.setExpression(var);
                break;
            case 1:
                statement.setExpression(clauses.get(0));
                break;
            default:
                InfixExpression expr = ast.newInfixExpression();
                expr.setLeftOperand(clauses.get(0));
                expr.setOperator(InfixExpression.Operator.AND);
                expr.setRightOperand(clauses.get(1));
                for (int i = 2; i < clauses.size(); i++) {
                    InfixExpression joined = ast.newInfixExpression();
                    joined.setLeftOperand(expr);
                    joined.setOperator(InfixExpression.Operator.AND);
                    joined.setRightOperand(clauses.get(i));
                    expr = joined;
                }
                statement.setExpression(expr);
        }

        /* synthesize the body */
        Block body = ast.newBlock();
        for (DASTNode dNode : _body) {
            ASTNode aNode = dNode.synthesize(env);
            if (aNode instanceof Statement)
                body.statements().add(aNode);
            else
                body.statements().add(ast.newExpressionStatement((Expression) aNode));
        }
        statement.setBody(body);

        return statement;
    }
}
