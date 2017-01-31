package synthesizer;

import dsl.DSubTree;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;

import java.util.*;

public class Visitor extends ASTVisitor {

    final DSubTree ast;
    final Document document;
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

    public Visitor(DSubTree ast, Document document) {
        this.ast = ast;
        this.document = document;
    }

    @Override
    public boolean visit(MethodDeclaration method) {
        if (! method.getName().getIdentifier().equals("__datasyn_fill"))
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
        ASTNode synthesizedBody = ast.synthesize(env);
        ASTRewrite rewriter = ASTRewrite.create(method.getBody().getAST());
        rewriter.replace(method.getBody(), synthesizedBody, null);
        TextEdit edits = rewriter.rewriteAST(document, null);
        try {
            edits.apply(document);
        } catch (BadLocationException e) {
            System.err.println("Could not edit document for some reason.\n" + e.getMessage());
            System.exit(1);
        }

        synthesizedProgram = document.get();

        return false;
    }
}
