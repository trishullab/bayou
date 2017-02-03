package dsl;

import org.eclipse.jdt.core.dom.*;
import synthesizer.Environment;
import synthesizer.Variable;

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

    @Override
    public String toString() {
        return "if (\n" + _cond + "\n) then {\n" + _then + "\n} else {\n" + _else + "\n}";
    }


    @Override
    public IfStatement synthesize(Environment env) {
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
