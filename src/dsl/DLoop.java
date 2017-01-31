package dsl;

import dom_driver.Visitor;
import org.eclipse.jdt.core.dom.*;
import synthesizer.Environment;
import synthesizer.Variable;

import java.util.ArrayList;
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
            Assignment assignment = call.synthesize(env);
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
                Variable v = env.searchOrAddVariable(boolean.class);
                SimpleName var = ast.newSimpleName(v.getName());
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
