package edu.rice.cs.caper.bayou.core.synthesizer;

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

    private static final Map<String,Class> primitiveToClass;
    static {
        Map<String,Class> map = new HashMap<>();
        map.put("int", int.class);
        map.put("long", long.class);
        map.put("double", double.class);
        map.put("float", float.class);
        map.put("boolean", boolean.class);
        map.put("char", char.class);
        map.put("byte", byte.class);
        map.put("void", void.class);
        map.put("short", short.class);
        primitiveToClass = Collections.unmodifiableMap(map);
    }

    public Visitor(DSubTree dAST, Document document, CompilationUnit cu) {
        this.dAST = dAST;
        this.document = document;
        this.cu = cu;

        this.rewriter = ASTRewrite.create(this.cu.getAST());
        this.currentScope = new ArrayList<>();
    }

    @Override
    public boolean visit(MethodInvocation invocation) {
        /* TODO: these checks are the same as in EvidenceExtractor. Make this process better. */
        IMethodBinding binding = invocation.resolveMethodBinding();
        if (binding == null)
            throw new RuntimeException("Could not resolve binding. " +
                    "Either CLASSPATH is not set correctly, or there is an invalid evidence type.");

        ITypeBinding cls = binding.getDeclaringClass();
        if (cls == null || !cls.getQualifiedName().equals("edu.rice.cs.caper.bayou.annotations.Evidence"))
            return false;

        if (! (invocation.getParent().getParent() instanceof Block))
            throw new RuntimeException("Evidence has to be given in a (empty) block.");
        Block evidenceBlock = (Block) invocation.getParent().getParent();
        if (this.evidenceBlock != null)
            if (this.evidenceBlock != evidenceBlock)
                throw new RuntimeException("Only one synthesis query at a time is supported.");
            else return false; /* synthesis is already done */
        this.evidenceBlock = evidenceBlock;

        String name = binding.getName();
        if (! (name.equals("apicalls") || name.equals("types") || name.equals("context")))
            throw new RuntimeException("Invalid evidence type: " + binding.getName());

        Environment env = new Environment(invocation.getAST(), currentScope);
        Block body;
        try {
            body = dAST.synthesize(env);
        } catch (SynthesisException e) {
            synthesizedProgram = null;
            return false;
        }

        // Apply dead code elimination here
        DCEOptimizor dce = new DCEOptimizor();
        //body = dce.apply(body);
	
        /* make rewrites to the local method body */
        body = postprocessLocal(invocation.getAST(), env, body, dce.getEliminatedVars());
        rewriter.replace(evidenceBlock, body, null);

        try {
            rewriter.rewriteAST(document, null).apply(document);

            /* make rewrites to the document */
            postprocessGlobal(cu.getAST(), env, document);
        } catch (BadLocationException e) {
            System.err.println("Could not edit document for some reason.\n" + e.getMessage());
            synthesizedProgram = null;
            return false;
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
        for (Variable var : env.mu_scope) {
	    if (!eliminatedVars.contains(var.name)) {
		VariableDeclarationFragment varDeclFrag = ast.newVariableDeclarationFragment();
		varDeclFrag.setName(ast.newSimpleName(var.name));
		VariableDeclarationStatement varDeclStmt = ast.newVariableDeclarationStatement(varDeclFrag);
		if (var.type.isPrimitive())
		    varDeclStmt.setType(ast.newPrimitiveType(PrimitiveType.toCode(var.type.getSimpleName())));
		else
		    varDeclStmt.setType(ast.newSimpleType(ast.newSimpleName(var.type.getSimpleName())));
		body.statements().add(0, varDeclStmt);
	    }
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
    public boolean visit(MethodDeclaration method) {
        currentScope.clear();

        /* add variables in the formal parameters */
        for (Object o : method.parameters()) {
            SingleVariableDeclaration param = (SingleVariableDeclaration) o;
            Type t = param.getType();
            Class type;

            if (t.isSimpleType()) {
                ITypeBinding binding = t.resolveBinding();
                if (binding == null)
                    continue;
                try {
                    type = Environment.getClass(binding.getQualifiedName());
                } catch (ClassNotFoundException  e) {
                    synthesizedProgram = null;
                    return false;
                }
            }
            else if (t.isPrimitiveType())
                type = primitiveToClass.get(((PrimitiveType) t).getPrimitiveTypeCode().toString());
            else continue;

            Variable v = new Variable(param.getName().getIdentifier(), type);
            currentScope.add(v);
        }

        /* add local variables declared in the (beginning of) method body */
        Block body = method.getBody();
        for (Object o : body.statements()) {
            Statement stmt = (Statement) o;
            if (! (stmt instanceof VariableDeclarationStatement))
                break; // stop at the first non-variable declaration
            VariableDeclarationStatement varDecl = (VariableDeclarationStatement) stmt;
            Type t = varDecl.getType();
            Class type;

            if (t.isSimpleType()) {
                ITypeBinding binding = t.resolveBinding();
                if (binding == null)
                    continue;
                try {
                    type = Environment.getClass(binding.getQualifiedName());
                } catch (ClassNotFoundException  e) {
                    synthesizedProgram = null;
                    return false;
                }
            }
            else if (t.isPrimitiveType())
                type = primitiveToClass.get(((PrimitiveType) t).getPrimitiveTypeCode().toString());
            else continue;

            for (Object f : varDecl.fragments()) {
                VariableDeclarationFragment frag = (VariableDeclarationFragment) f;
                Variable v = new Variable(frag.getName().getIdentifier(), type);
                currentScope.add(v);
            }
        }

        return true;
    }
}
