package edu.rice.bayou.annotations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.bayou.dsl.Sequence;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class EvidenceExtractor extends ASTVisitor {

    CommandLine cmdLine;

    class JSONOutputWrapper {
        String keywords;
        List<Sequence> sequences;

        public JSONOutputWrapper() {
            this.keywords = "";
            this.sequences = new ArrayList<>();
        }
    }

    JSONOutputWrapper output;

    public EvidenceExtractor(String[] args) {
        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.Options clopts = new org.apache.commons.cli.Options();

        addOptions(clopts);

        try {
            this.cmdLine = parser.parse(clopts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("edu/rice/bayou/evidence_extractor", clopts);
        }

        this.output = new JSONOutputWrapper();
    }

    private void addOptions(org.apache.commons.cli.Options opts) {
        opts.addOption(Option.builder("f")
                .longOpt("input-file")
                .hasArg()
                .numberOfArgs(1)
                .required()
                .desc("input Java program")
                .build());
    }

    public void execute() throws IOException {
        if (cmdLine == null)
            return;

        ASTParser parser = ASTParser.newParser(AST.JLS8);
        File input = new File(cmdLine.getOptionValue("input-file"));
        String classpath = System.getenv("CLASSPATH");

        String source = FileUtils.readFileToString(input, "utf-8");
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { classpath != null? classpath : "" },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
            cu.accept(this);
            System.out.println(gson.toJson(output));
        } catch (Exception e) {
            System.err.println("Unexpected error occurred. Make sure evidences are in the right format.");
            System.exit(1);
        }
    }

    @Override
    public boolean visit(MethodDeclaration method) {
        if (!method.getName().getIdentifier().equals("__bayou_fill"))
            return false;
        List<IExtendedModifier> modifiers = method.modifiers();

        // performing casts wildly.. if any exceptions occur it's due to incorrect input format
        for (IExtendedModifier m : modifiers) {
            if (! m.isAnnotation() || ! ((Annotation) m).isNormalAnnotation())
                continue;
            NormalAnnotation annotation = (NormalAnnotation) m;
            IAnnotationBinding aBinding = annotation.resolveAnnotationBinding();
            ITypeBinding binding;
            if (aBinding == null || (binding = aBinding.getAnnotationType()) == null)
                continue;
            if (! binding.getQualifiedName().equals("edu.rice.bayou.annotations.Evidence"))
                continue;
            List<MemberValuePair> values = annotation.values();

            for (MemberValuePair value : values) {
                String type = value.getName().getIdentifier();

                if (type.equals("keywords")) {
                    String val = ((StringLiteral) value.getValue()).getLiteralValue();
                    output.keywords += " " + val;
                }
                else if (type.equals("sequence")) {
                    Sequence sequence = new Sequence();
                    List<Expression> calls = ((ArrayInitializer) value.getValue()).expressions();
                    for (Expression e : calls) {
                        String call = ((StringLiteral) e).getLiteralValue();
                        sequence.addCall(call);
                    }
                    output.sequences.add(sequence);
                }
                else throw new RuntimeException();
            }
        }
        return false;
    }

    public static void main(String args[]) {
        try {
            new EvidenceExtractor(args).execute();
        } catch (IOException e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
