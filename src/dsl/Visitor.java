package dsl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jdt.core.dom.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Visitor extends ASTVisitor {

    final CompilationUnit unit;
    final Options options;
    final PrintWriter output;
    final Gson gson;

    class JSONOutputWrapper {
        DBlock ast;

        public JSONOutputWrapper(DBlock ast) {
            this.ast = ast;
        }
    }

    public Visitor(CompilationUnit unit, Options options) throws FileNotFoundException {
        this.unit = unit;
        this.options = options;
        this.gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

        if (options.cmdLine.hasOption("output-file"))
            this.output = new PrintWriter(options.cmdLine.getOptionValue("output-file"));
        else
            this.output = new PrintWriter(System.out);
    }

    @Override
    public boolean visit(MethodDeclaration method) {
        DBlock ast = new DBlock.Handle(method.getBody(), this).handle();
        if (ast != null)
            printJson(ast);
        return false;
    }

    boolean first = true;
    private void printJson(DBlock ast) {
        JSONOutputWrapper out = new JSONOutputWrapper(ast);
        output.write(first? "" : ",\n");
        output.write(gson.toJson(out));
        output.flush();
        first = false;
    }
}
