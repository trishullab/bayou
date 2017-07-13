package edu.rice.cs.caper.bayou.core.dsl;

import edu.rice.cs.caper.bayou.core.synthesizer.Environment;
import edu.rice.cs.caper.bayou.core.synthesizer.SynthesisException;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DBranch extends DASTNode {

    String node = "DBranch";
    List<DAPICall> _cond;
    List<DASTNode> _then;
    List<DASTNode> _else;

    public DBranch() {
        this._cond = new ArrayList<>();
        this._then = new ArrayList<>();
        this._else = new ArrayList<>();
        this.node = "DBranch";
    }

    public DBranch(List<DAPICall> _cond, List<DASTNode> _then, List<DASTNode> _else) {
        this._cond = _cond;
        this._then = _then;
        this._else = _else;
        this.node = "DBranch";
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length) throws TooManySequencesException, TooLongSequenceException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (DAPICall call : _cond)
            call.updateSequences(soFar, max, max_length);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.calls));
        for (DASTNode t : _then)
            t.updateSequences(soFar, max, max_length);
        for (DASTNode e : _else)
            e.updateSequences(copy, max, max_length);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
    }

    @Override
    public int numStatements() {
        int num = _cond.size();
        for (DASTNode t : _then)
            num += t.numStatements();
        for (DASTNode e : _else)
            num += e.numStatements();
        return num;
    }

    @Override
    public int numLoops() {
        int num = 0;
        for (DASTNode t : _then)
            num += t.numLoops();
        for (DASTNode e : _else)
            num += e.numLoops();
        return num;
    }

    @Override
    public int numBranches() {
        int num = 1; // this branch
        for (DASTNode t : _then)
            num += t.numBranches();
        for (DASTNode e : _else)
            num += e.numBranches();
        return num;
    }

    @Override
    public int numExcepts() {
        int num = 0;
        for (DASTNode t : _then)
            num += t.numExcepts();
        for (DASTNode e : _else)
            num += e.numExcepts();
        return num;
    }

    @Override
    public Set<DAPICall> bagOfAPICalls() {
        Set<DAPICall> bag = new HashSet<>();
        bag.addAll(_cond);
        for (DASTNode t : _then)
            bag.addAll(t.bagOfAPICalls());
        for (DASTNode e : _else)
            bag.addAll(e.bagOfAPICalls());
        return bag;
    }

    @Override
    public Set<Class> exceptionsThrown() {
        Set<Class> ex = new HashSet<>();
        for (DAPICall c : _cond)
            ex.addAll(c.exceptionsThrown());
        for (DASTNode t : _then)
            ex.addAll(t.exceptionsThrown());
        for (DASTNode e : _else)
            ex.addAll(e.exceptionsThrown());
        return ex;
    }

    @Override
    public Set<Class> exceptionsThrown(Set<String> eliminatedVars) {
	return this.exceptionsThrown();
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

    @Override
    public String toString() {
        return "if (\n" + _cond + "\n) then {\n" + _then + "\n} else {\n" + _else + "\n}";
    }


    @Override
    public IfStatement synthesize(Environment env) throws SynthesisException {
        AST ast = env.ast();
        IfStatement statement = ast.newIfStatement();

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

        /* synthesize then and else body */
        Block thenBlock = ast.newBlock();
        for (DASTNode dNode : _then) {
            ASTNode aNode = dNode.synthesize(env);
            if (aNode instanceof Statement)
                thenBlock.statements().add(aNode);
            else
                thenBlock.statements().add(ast.newExpressionStatement((Expression) aNode));
        }
        statement.setThenStatement(thenBlock);

        Block elseBlock = ast.newBlock();
        for (DASTNode dNode : _else) {
            ASTNode aNode = dNode.synthesize(env);
            if (aNode instanceof Statement)
                elseBlock.statements().add(aNode);
            else
                elseBlock.statements().add(ast.newExpressionStatement((Expression) aNode));
        }
        statement.setElseStatement(elseBlock);

        return statement;
    }
}
