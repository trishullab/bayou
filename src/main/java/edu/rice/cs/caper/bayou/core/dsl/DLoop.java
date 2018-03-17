/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.core.dsl;

import edu.rice.cs.caper.bayou.core.dom_driver.Visitor;
import edu.rice.cs.caper.bayou.core.synthesizer.*;
import edu.rice.cs.caper.bayou.core.synthesizer.Type;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DLoop extends DASTNode {

    String node = "DLoop";
    List<DAPICall> _cond;
    List<DASTNode> _body;

    public DLoop() {
        this._cond = new ArrayList<>();
        this._body = new ArrayList<>();
        this.node = "DLoop";
    }

    public DLoop(List<DAPICall> cond, List<DASTNode> _body) {
        this._cond = cond;
        this._body = _body;
        this.node = "DLoop";
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max, int max_length) throws TooManySequencesException, TooLongSequenceException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (DAPICall call : _cond)
            call.updateSequences(soFar, max, max_length);

        int num_unrolls = 1;
        for (int i = 0; i < num_unrolls; i++) {
            for (DASTNode node : _body)
                node.updateSequences(soFar, max, max_length);
            for (DAPICall call : _cond)
                call.updateSequences(soFar, max, max_length);
        }
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
    public Set<Class> exceptionsThrown(Set<String> eliminatedVars) {
	return this.exceptionsThrown();
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
    public WhileStatement synthesize(Environment env) throws SynthesisException {
        AST ast = env.ast();
        WhileStatement statement = ast.newWhileStatement();

        /* synthesize the condition */
        List<Expression> clauses = new ArrayList<>();
        for (DAPICall call : _cond) {
            ASTNode synth = call.synthesize(env);
            if (! (synth instanceof Assignment)) /* a call that returns void cannot be in condition */
                throw new SynthesisException(SynthesisException.MalformedASTFromNN);
            Assignment assignment = (Assignment) synth;

            ParenthesizedExpression pAssignment = ast.newParenthesizedExpression();
            pAssignment.setExpression(assignment);
            // if the method does not return a boolean, add != null or != 0 to the condition
            if (call.method == null || (!call.method.getReturnType().equals(Boolean.class) &&
                    !call.method.getReturnType().equals(boolean.class))) {
                InfixExpression notEqualsNull = ast.newInfixExpression();
                notEqualsNull.setLeftOperand(pAssignment);
                notEqualsNull.setOperator(InfixExpression.Operator.NOT_EQUALS);
                if (call.method != null && call.method.getReturnType().isPrimitive())
                    notEqualsNull.setRightOperand(ast.newNumberLiteral("0")); // primitive but not boolean
                else // some object
                    notEqualsNull.setRightOperand(ast.newNullLiteral());

                clauses.add(notEqualsNull);
            }
            else
                clauses.add(pAssignment);
        }
        switch (clauses.size()) {
            case 0:
                SearchTarget target = new SearchTarget(
                        new Type(ast.newPrimitiveType(PrimitiveType.toCode("boolean")), boolean.class));
                target.setSingleUseVariable(true);
                Expression var = env.search(target).getExpression();

                statement.setExpression(var);
                break;
            case 1:
                statement.setExpression(clauses.get(0));
                break;
            default:
                InfixExpression expr = ast.newInfixExpression();
                expr.setLeftOperand(clauses.get(0));
                expr.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
                expr.setRightOperand(clauses.get(1));
                for (int i = 2; i < clauses.size(); i++) {
                    InfixExpression joined = ast.newInfixExpression();
                    joined.setLeftOperand(expr);
                    joined.setOperator(InfixExpression.Operator.CONDITIONAL_AND);
                    joined.setRightOperand(clauses.get(i));
                    expr = joined;
                }
                statement.setExpression(expr);
        }

        /* synthesize the body under a new scope */
        env.pushScope();
        Block body = ast.newBlock();
        for (DASTNode dNode : _body) {
            ASTNode aNode = dNode.synthesize(env);
            if (aNode instanceof Statement)
                body.statements().add(aNode);
            else
                body.statements().add(ast.newExpressionStatement((Expression) aNode));
        }
        statement.setBody(body);

        /* join with parent scope itself (the "sub-scope" of a loop if condition was false) */
        List<Scope> scopes = new ArrayList<>();
        scopes.add(env.popScope());
        scopes.add(new Scope(env.getScope()));
        env.getScope().join(scopes);

        return statement;
    }
}
