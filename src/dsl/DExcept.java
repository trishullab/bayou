package dsl;

import org.eclipse.jdt.core.dom.*;
import synthesizer.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DExcept extends DASTNode {

    final String node = "DExcept";
    public List<DASTNode> _try;
    public List<DASTNode> _catch;

    public DExcept(List<DASTNode> _try, List<DASTNode> _catch) {
        this._try = _try;
        this._catch = _catch;
    }

    @Override
    public void updateSequences(List<Sequence> soFar) {
        for (DASTNode node : _try)
            node.updateSequences(soFar);
        List<Sequence> copy = new ArrayList<>();
        for (Sequence seq : soFar)
            copy.add(new Sequence(seq.calls));
        for (DASTNode e : _catch)
            e.updateSequences(copy);
        for (Sequence seq : copy)
            if (! soFar.contains(seq))
                soFar.add(seq);
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
        CatchClause catchClause = ast.newCatchClause();
        Block catchBlock = ast.newBlock();
        for (DASTNode dNode : _catch) {
            ASTNode aNode = dNode.synthesize(env);
            if (aNode instanceof Statement)
                catchBlock.statements().add(aNode);
            else
                catchBlock.statements().add(ast.newExpressionStatement((Expression) aNode));
        }
        catchClause.setBody(catchBlock);

        /* synthesize catch clause exception types */
        SingleVariableDeclaration ex = ast.newSingleVariableDeclaration();
        String exType = String.join("|", exceptionsThrown.stream().map(c -> c.getName()).collect(Collectors.toList()));
        ex.setType(ast.newSimpleType(ast.newName(exType)));
        ex.setName(ast.newSimpleName("_e"));
        catchClause.setException(ex);
        statement.catchClauses().add(catchClause);

        /* record exceptions that were caught */
        for (Class except : exceptionsThrown)
            env.recordExceptionCaught(except);

        return statement;
    }
}
