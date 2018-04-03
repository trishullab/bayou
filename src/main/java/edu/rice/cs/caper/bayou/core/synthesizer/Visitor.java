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

/**
 * Main class that implements the visitor pattern on the draft program's AST
 */
public class Visitor extends ASTVisitor {

    /**
     * The sketch to synthesize
     */
    private final DSubTree sketch;

    /**
     * The document object to store the synthesized code
     */
    private final Document document;

    /**
     * The compilation unit of the draft program
     */
    private final CompilationUnit cu;

    /**
     * Temporary store for the synthesized program. Gets updated with every invocation of visitor.
     */
    String synthesizedProgram;

    /**
     * The rewriter for the document
     */
    private ASTRewrite rewriter;

    /**
     * The block where the evidence is present (i.e., where the synthesized code should be placed)
     */
    private Block evidenceBlock;

    /**
     * Temporary list of variables in scope (formal params and local declarations) for synthesis.
     * Gets cleared and updated as each method declaration is visited.
     */
    private List<Variable> currentScope;

    /**
     * Temporary store for the return type of the method for synthesis.
     * Gets cleared and updated as each method declaration is visited.
     */
    private Type returnType;

    /**
     * Temporary store for the current method in which synthesis takes place.
     * Gets cleared and updated as each method declaration is visited.
     */
    private MethodDeclaration method;

    /**
     * The enumeration mode
     */
    private final Synthesizer.Mode mode;

    /**
     * Initializes the visitor
     *
     * @param sketch   sketch to be synthesized
     * @param document draft program document
     * @param cu       draft program compilation unit
     * @param mode     enumeration mode
     */
    public Visitor(DSubTree sketch, Document document, CompilationUnit cu, Synthesizer.Mode mode) {
        this.sketch = sketch;
        this.document = document;
        this.cu = cu;

        this.rewriter = ASTRewrite.create(this.cu.getAST());
        this.currentScope = new ArrayList<>();
        this.mode = mode;
    }

    /**
     * Visits a method declaration and sets up the environment that may be used for synthesis:
     * - the variables in scope (formal parameters, local declarations)
     * - the return type of the method
     * - the method itself (to update formal parameters if needed)
     *
     * @param method the method declaration being visited
     * @return boolean indicating if the AST node should be explored further
     * @throws SynthesisException if there is an error with creating necessary types
     */
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
            if (!(stmt instanceof VariableDeclarationStatement))
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

        /* set the return type */
        returnType = new Type(method.getReturnType2());

        /* store the current method */
        this.method = method;

        return true;
    }

    /**
     * Visits a method invocation and triggers synthesis of "sketch" if it's an evidence block
     *
     * @param invocation a method invocation
     * @return boolean indicating if the AST node should be explored further
     * @throws SynthesisException if evidence declaration is illegal
     */
    @Override
    public boolean visit(MethodInvocation invocation) throws SynthesisException {
        IMethodBinding binding = invocation.resolveMethodBinding();
        if (binding == null)
            throw new SynthesisException(SynthesisException.CouldNotResolveBinding);

        ITypeBinding cls = binding.getDeclaringClass();
        if (cls == null || !cls.getQualifiedName().equals("edu.rice.cs.caper.bayou.annotations.Evidence"))
            return false;

        if (!(invocation.getParent().getParent() instanceof Block))
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
        if (!(name.equals("apicalls") || name.equals("types") || name.equals("keywords")))
            throw new SynthesisException(SynthesisException.InvalidEvidenceType);

        Environment env = new Environment(invocation.getAST(), currentScope, mode);
        Block body = sketch.synthesize(env);

        // Apply dead code elimination here
        DCEOptimizor dce = new DCEOptimizor();
        body = dce.apply(body, sketch);
        if (body.statements().size() == 0)
            return false;

        /* make rewrites to the local method body */
        body = postprocessLocal(method.getAST(), env, body, dce.getEliminatedVars());
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

    /**
     * Performs local post-processing of synthesized code:
     * - Adds try-catch for uncaught exceptions
     * - Adds local variable declarations
     * - Adds return statement
     *
     * @param ast            the owner of the block
     * @param env            environment that was used for synthesis
     * @param body           block containing synthesized code
     * @param eliminatedVars variables eliminiated by DCE
     * @return updated block containing synthesized code
     */
    private Block postprocessLocal(AST ast, Environment env, Block body, Set<String> eliminatedVars) {
        /* add uncaught exeptions */
        Set<Class> exceptions = sketch.exceptionsThrown(eliminatedVars);
        env.getImports().addAll(exceptions);
        if (!exceptions.isEmpty()) {
            TryStatement statement = ast.newTryStatement();
            statement.setBody(body);

            List<Class> exceptions_ = new ArrayList<>(exceptions);
            exceptions_.sort((Class e1, Class e2) -> e1.isAssignableFrom(e2) ? 1 : -1);
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
        ListRewrite paramsRewriter = rewriter.getListRewrite(method, MethodDeclaration.PARAMETERS_PROPERTY);
        for (Variable var : toDeclare) {
            if (eliminatedVars.contains(var.getName()) || var.isUserVar())
                continue;

            // add the variable declaration to either the method's formal params or the body
            org.eclipse.jdt.core.dom.Type varDeclType = var.getType().simpleT(ast, env);
            if (var.isDefaultInit()) {
                SingleVariableDeclaration varDecl = ast.newSingleVariableDeclaration();
                varDecl.setType(varDeclType);
                varDecl.setName(var.createASTNode(ast));
                var.refactor("_" + var.getName());
                paramsRewriter.insertLast(varDecl, null);
            } else {
                VariableDeclarationFragment varDeclFrag = ast.newVariableDeclarationFragment();
                varDeclFrag.setName(var.createASTNode(ast));
                VariableDeclarationStatement varDeclStmt = ast.newVariableDeclarationStatement(varDeclFrag);
                varDeclStmt.setType(varDeclType);
                body.statements().add(0, varDeclStmt);
            }
        }

        /* add return statement */
        org.eclipse.jdt.core.dom.Type ret = returnType.T();
        List<Variable> toReturn = new ArrayList<>();
        for (Variable var : env.getScope().getVariables())
            if (returnType.isAssignableFrom(var.getType()))
                toReturn.add(var);
        toReturn.sort(Comparator.comparingInt(v -> v.getRefCount()));

        ReturnStatement returnStmt = ast.newReturnStatement();
        if (toReturn.isEmpty()) { // add "return null" (or primitive) in order to make the code compile
            if (ret.isPrimitiveType()) {
                PrimitiveType primitiveType = (PrimitiveType) ret;
                if (primitiveType.getPrimitiveTypeCode() == PrimitiveType.BOOLEAN)
                    returnStmt.setExpression(ast.newBooleanLiteral(false));
                else if (primitiveType.getPrimitiveTypeCode() != PrimitiveType.VOID)
                    returnStmt.setExpression(ast.newNumberLiteral("0"));
            } else
                returnStmt.setExpression(ast.newNullLiteral());
        } else {
            returnStmt.setExpression(toReturn.get(0).createASTNode(ast));
        }
        body.statements().add(returnStmt);

        return body;
    }

    /**
     * Performs global post-processing of synthesized code:
     * - Adds import declarations
     *
     * @param ast      the owner of the document
     * @param env      environment that was used for synthesis
     * @param document draft program document
     * @throws BadLocationException if an error occurred when rewriting document
     */
    private void postprocessGlobal(AST ast, Environment env, Document document)
            throws BadLocationException {
        /* add imports */
        ASTRewrite rewriter = ASTRewrite.create(ast);
        ListRewrite lrw = rewriter.getListRewrite(cu, CompilationUnit.IMPORTS_PROPERTY);
        Set<Class> toImport = new HashSet<>(env.getImports());
        toImport.addAll(sketch.exceptionsThrown()); // add all catch(...) types to imports
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
}
