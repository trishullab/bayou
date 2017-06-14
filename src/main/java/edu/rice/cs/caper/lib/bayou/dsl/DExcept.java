package edu.rice.cs.caper.lib.bayou.dsl;

import edu.rice.cs.caper.lib.bayou.synthesizer.Environment;
import edu.rice.cs.caper.lib.bayou.synthesizer.Variable;
import org.eclipse.jdt.core.dom.*;


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
    public void updateSequences(List<Sequence> soFar, int max, int max_length)  throws TooManySequencesException, TooLongSequenceException {
        if (soFar.size() >= max)
            throw new TooManySequencesException();
        for (DASTNode node : _try)
            node.updateSequences(soFar, max, max_length);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.calls));
        for (DASTNode e : _catch)
            e.updateSequences(copy, max, max_length);
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
    public Set<Class> exceptionsThrown() {
        Set<Class> ex = new HashSet<>();
        // no try: whatever thrown in try would have been caught in catch
        for (DASTNode c : _catch)
            ex.addAll(c.exceptionsThrown());
        return ex;
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
    public TryStatement synthesize(Environment env) throws ClassNotFoundException, BindingNotFoundException
    {
        AST ast = env.ast();
        TryStatement statement = ast.newTryStatement();

        /* synthesize try block */
        Block tryBlock = ast.newBlock();
        Set<Class> exceptionsThrown = new HashSet<>();
        for (DASTNode dNode : _try) {
            ASTNode aNode = dNode.synthesize(env);
            if (aNode instanceof Statement)
                tryBlock.statements().add(aNode);
            else
                tryBlock.statements().add(ast.newExpressionStatement((Expression) aNode));

            exceptionsThrown.addAll(dNode.exceptionsThrown());
        }
        statement.setBody(tryBlock);
        List<Class> exceptionsThrown_ = new ArrayList<>(exceptionsThrown);
        exceptionsThrown_.sort((Class e1, Class e2) -> e1.isAssignableFrom(e2)? 1: -1);

        /* synthesize catch clause body */
        for (Class except : exceptionsThrown_) {
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
            env.addImport(except);
        }

        return statement;
    }
}
