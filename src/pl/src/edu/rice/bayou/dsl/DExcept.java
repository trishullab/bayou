package edu.rice.bayou.dsl;

import org.eclipse.jdt.core.dom.*;
import edu.rice.bayou.synthesizer.Environment;
import edu.rice.bayou.synthesizer.Variable;

import java.util.*;

public class DExcept extends DASTNode {

    final String node = "DExcept";
    public List<DASTNode> _try;
    public List<DASTNode> _catch;

    public DExcept(List<DASTNode> _try, List<DASTNode> _catch) {
        this._try = _try;
        this._catch = _catch;
    }

    @Override
    public void updateSequences(List<Sequence> soFar, int max)  throws TooManySequencesException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (DASTNode node : _try)
            node.updateSequences(soFar, max);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.calls));
        for (DASTNode e : _catch)
            e.updateSequences(copy, max);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
    }

    @Override
    public int numStatements() {
        int num = _try.size();
        for (DASTNode c : _catch)
            num += c.numStatements();
        return num;
    }

    @Override
    public int numLoops() {
        int num = 0;
        for (DASTNode t : _try)
            num += t.numLoops();
        for (DASTNode c : _catch)
            num += c.numLoops();
        return num;
    }

    @Override
    public int numBranches() {
        int num = 0;
        for (DASTNode t : _try)
            num += t.numBranches();
        for (DASTNode c : _catch)
            num += c.numBranches();
        return num;
    }

    @Override
    public int numExcepts() {
        int num = 1; // this except
        for (DASTNode t : _try)
            num += t.numExcepts();
        for (DASTNode c : _catch)
            num += c.numExcepts();
        return num;
    }

    @Override
    public Set<DAPICall> bagOfAPICalls() {
        Set<DAPICall> bag = new HashSet<>();
        for (DASTNode t : _try)
            bag.addAll(t.bagOfAPICalls());
        for (DASTNode c : _catch)
            bag.addAll(c.bagOfAPICalls());
        return bag;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || ! (o instanceof DExcept))
            return false;
        DExcept other = (DExcept) o;
        return _try.equals(other._try) && _catch.equals(other._catch);
    }

    @Override
    public int hashCode() {
        return 7* _try.hashCode() + 17* _catch.hashCode();
    }

    @Override
    public String toString() {
        return "try {\n" + _try + "\n} catch {\n" + _catch + "\n}";
    }



    @Override
    public TryStatement synthesize(Environment env) {
        AST ast = env.ast();
        TryStatement statement = ast.newTryStatement();

        /* synthesize try block */
        Block tryBlock = ast.newBlock();
        List<Class> exceptionsThrown = new ArrayList<>();
        for (DASTNode dNode : _try) {
            ASTNode aNode = dNode.synthesize(env);
            if (aNode instanceof Statement)
                tryBlock.statements().add(aNode);
            else
                tryBlock.statements().add(ast.newExpressionStatement((Expression) aNode));

            if (dNode instanceof DAPICall) {
                DAPICall call = (DAPICall) dNode;
                if (call.constructor != null)
                    exceptionsThrown.addAll(Arrays.asList(call.constructor.getExceptionTypes()));
                else
                    exceptionsThrown.addAll(Arrays.asList(call.method.getExceptionTypes()));
            }
        }
        statement.setBody(tryBlock);

        /* synthesize catch clause body */
        for (Class except : exceptionsThrown) {
            CatchClause catchClause = ast.newCatchClause();

            /* synthesize catch clause exception types */
            SingleVariableDeclaration ex = ast.newSingleVariableDeclaration();
            ex.setType(ast.newSimpleType(ast.newName(except.getSimpleName())));
            ex.setName(ast.newSimpleName("_e"));
            catchClause.setException(ex);
            statement.catchClauses().add(catchClause);
            Variable exceptionVar = env.addScopedVariable("_e", except);

            Block catchBlock = ast.newBlock();
            for (DASTNode dNode : _catch) {
                ASTNode aNode = dNode.synthesize(env);
                if (aNode instanceof Statement)
                    catchBlock.statements().add(aNode);
                else
                    catchBlock.statements().add(ast.newExpressionStatement((Expression) aNode));
            }
            catchClause.setBody(catchBlock);
            env.removeScopedVariable(exceptionVar);

            /* record exceptions that were caught */
            env.recordExceptionCaught(except);
        }

        return statement;
    }
}
