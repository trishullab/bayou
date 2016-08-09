package dsl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jdt.core.dom.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class Visitor extends ASTVisitor {

    CompilationUnit unit;
    Options options;
    final PrintWriter output;

    public Visitor(CompilationUnit unit, Options options) throws FileNotFoundException {
        this.unit = unit;
        this.options = options;

        if (options.cmdLine.hasOption("output-file"))
            output = new PrintWriter(options.cmdLine.getOptionValue("output-file"));
        else
            output = new PrintWriter(System.out);
    }

    @Override
    public boolean visit(MethodDeclaration method) {
        DBlock ast = new DBlock.Handle(method.getBody(), this).handle();
        if (ast != null)
            printJson(ast);
        return false;
    }

    private void printJson(DBlock ast) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        output.write(gson.toJson(ast));
        output.write("\n");
        output.flush();
    }
}
