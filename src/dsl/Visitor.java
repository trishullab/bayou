package dsl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.eclipse.jdt.core.dom.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class Visitor extends ASTVisitor {

    final CompilationUnit unit;
    final Options options;
    final PrintWriter output;
    final Gson gson;

    class JSONOutputWrapper {
        DBlock ast;
        List<Sequence> sequences;

        public JSONOutputWrapper(DBlock ast, List<Sequence> sequences) {
            this.ast = ast;
            this.sequences = sequences;
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
        if (ast != null) {
            List<Sequence> sequences = new ArrayList<>();
            sequences.add(new Sequence());
            new DBlock.Handle(method.getBody(), this).updateSequences(sequences);

            printJson(ast, sequences);
        }
        return false;
    }

    boolean first = true;
    private void printJson(DBlock ast, List<Sequence> sequences) {
        JSONOutputWrapper out = new JSONOutputWrapper(ast, sequences);
        output.write(first? "" : ",\n");
        output.write(gson.toJson(out));
        output.flush();
        first = false;
    }
}
