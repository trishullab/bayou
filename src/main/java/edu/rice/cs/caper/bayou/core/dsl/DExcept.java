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

import edu.rice.cs.caper.bayou.core.synthesizer.*;
import edu.rice.cs.caper.bayou.core.synthesizer.Type;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

public class DExcept extends DASTNode {

    String node = "DExcept";
    List<DASTNode> _try;
    List<DASTNode> _catch;
    transient Map exceptToClause;

    public DExcept() {
        this._try = new ArrayList<>();
        this._catch = new ArrayList<>();
        this.node = "DExcept";
    }

    public DExcept(List<DASTNode> _try, List<DASTNode> _catch) {
        this._try = _try;
        this._catch = _catch;
        this.node = "DExcept";
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
    public Set<Class> exceptionsThrown(Set<String> eliminatedVars) {
        return this.exceptionsThrown();
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
    public TryStatement synthesize(Environment env) throws SynthesisException {
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

        if (this.exceptToClause == null)
            this.exceptToClause = new HashMap<>();

        if (exceptionsThrown_.isEmpty()) /* at least one exception must be thrown in try block */
            throw new SynthesisException(SynthesisException.MalformedASTFromNN);

        /* synthesize catch clause body */
        List<Scope> scopes = new ArrayList<>();
        for (Class except : exceptionsThrown_) {
            CatchClause catchClause = ast.newCatchClause();

            /* push a new scope with the catch(... e) variable */
            env.pushScope();
            Type type = new Type(except);
            type.concretizeType(env); // shouldn't be a generic type
            SimpleName name = (SimpleName) env.addVariable(type, false).getExpression();

            /* synthesize catch clause exception types */
            SingleVariableDeclaration ex = ast.newSingleVariableDeclaration();
            ex.setType(ast.newSimpleType(ast.newName(except.getSimpleName())));
            ex.setName(name);
            catchClause.setException(ex);
            statement.catchClauses().add(catchClause);

            Block catchBlock = ast.newBlock();
            for (DASTNode dNode : _catch) {
                ASTNode aNode = dNode.synthesize(env);
                if (aNode instanceof Statement)
                    catchBlock.statements().add(aNode);
                else
                    catchBlock.statements().add(ast.newExpressionStatement((Expression) aNode));
            }
            catchClause.setBody(catchBlock);
            scopes.add(env.popScope());
            env.addImport(except);

            this.exceptToClause.put(except, catchClause);
        }

        env.getScope().join(scopes);
        return statement;
    }

    public void cleanupCatchClauses(Set<String> eliminatedVars) {
        Set excepts = new HashSet<>();
        for (DASTNode tn : _try) {
            if (tn instanceof DAPICall) {
                String retVarName = ((DAPICall)tn).getRetVarName();
                if (!retVarName.equals("") && eliminatedVars.contains(retVarName))
                    excepts.addAll(tn.exceptionsThrown());
            }
        }

        for (Object obj : this.exceptToClause.keySet()) {
            if (excepts.contains(obj)) {
                CatchClause catchClause = (CatchClause)this.exceptToClause.get(obj);
                catchClause.delete();
            }
        }
    }
}
