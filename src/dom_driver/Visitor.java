package dom_driver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dsl.DSubTree;
import dsl.Sequence;
import org.eclipse.jdt.core.dom.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

public class Visitor extends ASTVisitor {

    public final CompilationUnit unit;
    public final Options options;
    public final PrintWriter output;
    public final Gson gson;

    public List<MethodDeclaration> allMethods;

    private static Visitor V;

    public static Visitor V() {
        return V;
    }

    class JSONOutputWrapper {
        String file;
        DSubTree ast;
        List<Sequence> sequences;

        public JSONOutputWrapper(String file, DSubTree ast, List<Sequence> sequences) {
            this.file = file;
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

        allMethods = new ArrayList<>();
        V = this;
    }

    @Override
    public boolean visit(TypeDeclaration clazz) {
        if (clazz.isInterface())
            return false;
        List<TypeDeclaration> classes = new ArrayList<>();
        classes.addAll(Arrays.asList(clazz.getTypes()));
        classes.add(clazz);

        for (TypeDeclaration cls : classes)
            allMethods.addAll(Arrays.asList(cls.getMethods()));
        List<MethodDeclaration> constructors = allMethods.stream().filter(m -> m.isConstructor()).collect(Collectors.toList());
        List<MethodDeclaration> publicMethods = allMethods.stream().filter(m -> !m.isConstructor() && Modifier.isPublic(m.getModifiers())).collect(Collectors.toList());

        Set<DSubTree> asts = new HashSet<>();
        if (! constructors.isEmpty())
            for (MethodDeclaration c : constructors)
                for (MethodDeclaration m : publicMethods) {
                    DSubTree ast = new DOMMethodDeclaration(c).handle();
                    ast.addNodes(new DOMMethodDeclaration(m).handle().getNodes());
                    if (ast.isValid())
                        asts.add(ast);
                }
        else // no constructors, methods executed typically through Android callbacks
            for (MethodDeclaration m : publicMethods) {
                DSubTree ast = new DOMMethodDeclaration(m).handle();
                if (ast.isValid())
                    asts.add(ast);
            }

        for (DSubTree ast : asts) {
            List<Sequence> sequences = new ArrayList<>();
            sequences.add(new Sequence());
            ast.updateSequences(sequences);

            List<Sequence> uniqSequences = new ArrayList<>(new HashSet<>(sequences));
            printJson(ast, uniqSequences);
        }
        return false;
    }

    boolean first = true;
    private void printJson(DSubTree ast, List<Sequence> sequences) {
        String file = options.cmdLine.getOptionValue("input-file");
        JSONOutputWrapper out = new JSONOutputWrapper(file, ast, sequences);
        output.write(first? "" : ",\n");
        output.write(gson.toJson(out));
        output.flush();
        first = false;
    }
}
