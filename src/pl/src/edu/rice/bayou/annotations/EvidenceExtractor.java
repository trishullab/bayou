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
        List<String> apicalls;
        List<String> types;
        List<String> context;

        public JSONOutputWrapper() {
            this.apicalls = new ArrayList<>();
            this.types = new ArrayList<>();
            this.context = new ArrayList<>();
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
    public boolean visit(MethodInvocation invoke) {
	// We only handle the method invocation with special name __bayou_evidence
	if (!invoke.toString().startsWith("__bayou_evidence("))
	    return false;

	// Extacting invoke arguments
	for (Object argObj : invoke.arguments()) {
	    EvidenceObject evidObj = new EvidenceObject(argObj.toString());
	    if (evidObj.getType().equals("apicalls")) {
		for (String elem : evidObj.getElements()) {
		    output.apicalls.add(elem);
		}
	    } else if (evidObj.getType().equals("types")) {
		for (String elem : evidObj.getElements()) {
                    output.types.add(elem);
                }
	    } else if (evidObj.getType().equals("context")) {
		for (String elem : evidObj.getElements()) {
                    output.context.add(elem);
                }
	    }
	}

	return false;
    }

    @Override
    public boolean visit(MethodDeclaration method) {
        // if (!method.getName().getIdentifier().equals("__bayou_fill"))
	// return false;

        /* 1. Get apicalls and types from @Evidence annotation */
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

                if (type.equals("apicalls")) {
                    List<Expression> apicalls = ((ArrayInitializer) value.getValue()).expressions();
                    for (Expression e : apicalls) {
                        String a = ((StringLiteral) e).getLiteralValue();
                        output.apicalls.add(a);
                    }
                }
                else if (type.equals("types")) {
                    List<Expression> types = ((ArrayInitializer) value.getValue()).expressions();
                    for (Expression e : types) {
                        String t = ((StringLiteral) e).getLiteralValue();
                        output.types.add(t);
                    }
                }
                else if (type.equals("context")) {
                    List<Expression> context = ((ArrayInitializer) value.getValue()).expressions();
                    for (Expression e : context) {
                        String c = ((StringLiteral) e).getLiteralValue();
                        output.context.add(c);
                    }
                }
                else throw new RuntimeException();
            }
        }

        return true;
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
