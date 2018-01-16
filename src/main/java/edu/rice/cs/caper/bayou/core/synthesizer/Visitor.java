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
package edu.rice.cs.caper.bayou.core.synthesizer;

import edu.rice.cs.caper.bayou.annotations.Bayou;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import java.util.*;

public class Visitor extends ASTVisitor {

    final DSubTree dAST;
    final Document document;
    final CompilationUnit cu;
    String synthesizedProgram;
    protected ASTRewrite rewriter;
    Block evidenceBlock;
    List<Variable> currentScope;
    final Synthesizer.Mode mode;

    public Visitor(DSubTree dAST, Document document, CompilationUnit cu, Synthesizer.Mode mode) {
        this.dAST = dAST;
        this.document = document;
        this.cu = cu;

        this.rewriter = ASTRewrite.create(this.cu.getAST());
        this.currentScope = new ArrayList<>();
        this.mode = mode;
    }

    @Override
    public boolean visit(MethodInvocation invocation) throws SynthesisException {
        IMethodBinding binding = invocation.resolveMethodBinding();
        if (binding == null)
            throw new SynthesisException(SynthesisException.CouldNotResolveBinding);

        ITypeBinding cls = binding.getDeclaringClass();
        if (cls == null || !cls.getQualifiedName().equals("edu.rice.cs.caper.bayou.annotations.Evidence"))
            return false;

        if (! (invocation.getParent().getParent() instanceof Block))
            throw new SynthesisException(SynthesisException.EvidenceNotInBlock);
        Block evidenceBlock = (Block) invocation.getParent().getParent();

        if (!EvidenceExtractor.isLegalEvidenceBlock(evidenceBlock))
            throw new SynthesisException(SynthesisException.EvidenceMixedWithCode);

        if (this.evidenceBlock != null)
            if (this.evidenceBlock != evidenceBlock)
                throw new SynthesisException(SynthesisException.MoreThanOneHole);
            else return false; /* synthesis is already done */
        this.evidenceBlock = evidenceBlock;

        String name = binding.getName();
        if (! (name.equals("apicalls") || name.equals("types") || name.equals("keywords")))
            throw new SynthesisException(SynthesisException.InvalidEvidenceType);

        Environment env = new Environment(invocation.getAST(), currentScope, mode);
        Block body = dAST.synthesize(env);

        // Apply dead code elimination here
        DCEOptimizor dce = new DCEOptimizor();
        body = dce.apply(body, dAST);
	
        /* make rewrites to the local method body */
        body = postprocessLocal(invocation.getAST(), env, body, dce.getEliminatedVars());
        rewriter.replace(evidenceBlock, body, null);

        try {
            rewriter.rewriteAST(document, null).apply(document);

            /* make rewrites to the document */
            postprocessGlobal(cu.getAST(), env, document);
        } catch (BadLocationException e) {
            throw new SynthesisException(SynthesisException.CouldNotEditDocument);
        }

        synthesizedProgram = document.get();

        return false;
    }

    private Block postprocessLocal(AST ast, Environment env, Block body, Set<String> eliminatedVars) {
        /* add uncaught exeptions */
        Set<Class> exceptions = dAST.exceptionsThrown(eliminatedVars);
        env.imports.addAll(exceptions);
        if (! exceptions.isEmpty()) {
            TryStatement statement = ast.newTryStatement();
            statement.setBody(body);

            List<Class> exceptions_ = new ArrayList<>(exceptions);
            exceptions_.sort((Class e1, Class e2) -> e1.isAssignableFrom(e2)? 1: -1);
            for (Class except : exceptions_) {
                CatchClause catchClause = ast.newCatchClause();
                SingleVariableDeclaration ex = ast.newSingleVariableDeclaration();
                ex.setType(ast.newSimpleType(ast.newName(except.getSimpleName())));
                ex.setName(ast.newSimpleName("_e"));
                catchClause.setException(ex);
                catchClause.setBody(ast.newBlock());
                statement.catchClauses().add(catchClause);
            }

            body = ast.newBlock();
            body.statements().add(statement);
        }

        /* add variable declarations */
        Set<Variable> toDeclare = env.getScope().getVariables();
        toDeclare.addAll(env.getScope().getPhantomVariables());
        for (Variable var : toDeclare) {
            if (eliminatedVars.contains(var.getName()) || var.isUserVar())
                continue;

            // create the variable declaration fragment
            VariableDeclarationFragment varDeclFrag = ast.newVariableDeclarationFragment();
            varDeclFrag.setName(var.createASTNode(ast));

            // set the default initializer if the variable is a dollar variable
            if (var.isDefaultInit()) {
                env.addImport(Bayou.class); // import the "Bayou" class in Bayou
                varDeclFrag.setInitializer(var.createDefaultInitializer(ast));
            }

            // set the type for the statement
            VariableDeclarationStatement varDeclStmt = ast.newVariableDeclarationStatement(varDeclFrag);
            if (var.getType().T().isPrimitiveType())
                varDeclStmt.setType((org.eclipse.jdt.core.dom.Type) ASTNode.copySubtree(ast, var.getType().T()));
            else if (var.getType().T().isSimpleType()) {
                Name name = ((SimpleType) ASTNode.copySubtree(ast, var.getType().T())).getName();
                String simpleName = name.isSimpleName()? ((SimpleName) name).getIdentifier()
                        : ((QualifiedName) name).getName().getIdentifier();
                varDeclStmt.setType(ast.newSimpleType(ast.newSimpleName(simpleName)));
            }
            else if (var.getType().T().isParameterizedType() || var.getType().T().isArrayType()) {
                varDeclStmt.setType((org.eclipse.jdt.core.dom.Type) ASTNode.copySubtree(ast, var.getType().T()));
            }
            else throw new SynthesisException(SynthesisException.InvalidKindOfType);

            body.statements().add(0, varDeclStmt);
        }

        return body;
    }

    private void postprocessGlobal(AST ast, Environment env, Document document)
            throws BadLocationException {
        /* add imports */
        ASTRewrite rewriter = ASTRewrite.create(ast);
        ListRewrite lrw = rewriter.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
        Set<Class> toImport = new HashSet<>(env.imports);
        toImport.addAll(dAST.exceptionsThrown()); // add all catch(...) types to imports
        for (Class cls : toImport) {
            while (cls.isArray())
                cls = cls.getComponentType();
            if (cls.isPrimitive() || cls.getPackage().getName().equals("java.lang"))
                continue;
            ImportDeclaration impDecl = cu.getAST().newImportDeclaration();
            String className = cls.getName().replaceAll("\\$", "\\.");
            impDecl.setName(cu.getAST().newName(className.split("\\.")));
            lrw.insertLast(impDecl, null);
        }
        rewriter.rewriteAST(document, null).apply(document);
    }

    /* setup the scope of variables for synthesis */
    @Override
    public boolean visit(MethodDeclaration method) throws SynthesisException {
        currentScope.clear();

        /* add variables in the formal parameters */
        for (Object o : method.parameters()) {
            SingleVariableDeclaration param = (SingleVariableDeclaration) o;
            String name = param.getName().getIdentifier();
            Type type = new Type(param.getType());
            VariableProperties properties = new VariableProperties().setUserVar(true);
            Variable v = new Variable(name, type, properties);
            currentScope.add(v);
        }

        /* add local variables declared in the (beginning of) method body */
        Block body = method.getBody();
        for (Object o : body.statements()) {
            Statement stmt = (Statement) o;
            if (! (stmt instanceof VariableDeclarationStatement))
                break; // stop at the first non-variable declaration
            VariableDeclarationStatement varDecl = (VariableDeclarationStatement) stmt;
            for (Object f : varDecl.fragments()) {
                VariableDeclarationFragment frag = (VariableDeclarationFragment) f;
                String name = frag.getName().getIdentifier();
                Type type = new Type(varDecl.getType());
                VariableProperties properties = new VariableProperties().setUserVar(true);
                Variable v = new Variable(name, type, properties);
                currentScope.add(v);
            }
        }

        return true;
    }
}
