package edu.rice.cs.caper.lib.bayou.synthesizer;

import edu.rice.cs.caper.lib.bayou.dsl.DSubTree;
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
    }

    @Override
    public boolean visit(MethodDeclaration method) {
        if (! method.getName().getIdentifier().equals("__bayou_fill"))
            return false;

        List<Variable> scope = new ArrayList<>();
        for (Object o : method.parameters()) {
            SingleVariableDeclaration param = (SingleVariableDeclaration) o;
            Type t = param.getType();
            Class type;

            if (t.isSimpleType()) {
                ITypeBinding binding = t.resolveBinding();
                if (binding == null)
                    continue;
                type = Environment.getClass(binding.getQualifiedName());
            }
            else if (t.isPrimitiveType())
                type = primitiveToClass.get(((PrimitiveType) t).getPrimitiveTypeCode().toString());
            else continue;

            Variable v = new Variable(param.getName().getIdentifier(), type);
            scope.add(v);
        }

        Environment env = new Environment(method.getAST(), scope);
        Block body = dAST.synthesize(env);

        try {
            /* make rewrites to the local method body */
            body = postprocessLocal(method.getAST(), env, body);
            ASTRewrite rewriter = ASTRewrite.create(method.getAST());
            rewriter.replace(method.getBody(), body, null);

            /* remove the evidence annotations */
            List<IExtendedModifier> modifiers = method.modifiers();
            for (IExtendedModifier m : modifiers) {
                if (!m.isAnnotation() || !((Annotation) m).isNormalAnnotation())
                    continue;
                NormalAnnotation annotation = (NormalAnnotation) m;
                IAnnotationBinding aBinding = annotation.resolveAnnotationBinding();
                ITypeBinding binding;
                if (aBinding == null || (binding = aBinding.getAnnotationType()) == null)
                    continue;
                if (binding.getQualifiedName().equals("edu.rice.bayou.annotations.Evidence"))
                    rewriter.remove(annotation, null);
            }
            rewriter.rewriteAST(document, null).apply(document);

            /* make rewrites to the document */
            postprocessGlobal(cu.getAST(), env, document);
        } catch (BadLocationException e) {
            System.err.println("Could not edit document for some reason.\n" + e.getMessage());
            System.exit(1);
        }

        synthesizedProgram = document.get();

        return false;
    }

    private Block postprocessLocal(AST ast, Environment env, Block body) {
        /* add uncaught exeptions */
        Set<Class> exceptions = dAST.exceptionsThrown();
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
            VariableDeclarationFragment varDeclFrag = ast.newVariableDeclarationFragment();
            varDeclFrag.setName(ast.newSimpleName(var.name));
            VariableDeclarationStatement varDeclStmt = ast.newVariableDeclarationStatement(varDeclFrag);
            if (var.type.isPrimitive())
                varDeclStmt.setType(ast.newPrimitiveType(PrimitiveType.toCode(var.type.getSimpleName())));
            else
                varDeclStmt.setType(ast.newSimpleType(ast.newSimpleName(var.type.getSimpleName())));
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
