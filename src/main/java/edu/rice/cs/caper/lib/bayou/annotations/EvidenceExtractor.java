package edu.rice.cs.caper.lib.bayou.annotations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.lib.bayou.dsl.Sequence;
import org.eclipse.jdt.core.dom.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

public class EvidenceExtractor extends ASTVisitor {

    /**
     * Place to send logging information.
     */
    private static final Logger _logger = LogManager.getLogger(EvidenceExtractor.class.getName());

  //  CommandLine cmdLine;

    class JSONOutputWrapper {
        String keywords;
        List<Sequence> sequences;
        String javadoc;
        List<String> types;

        public JSONOutputWrapper() {
            this.keywords = "";
            this.sequences = new ArrayList<>();
            this.javadoc = "";
            this.types = new ArrayList<>();
        }
    }

    JSONOutputWrapper output = new JSONOutputWrapper();


    public String execute(String source, String classpath) {

        _logger.trace("classpath:" + classpath);

        ASTParser parser = ASTParser.newParser(AST.JLS8);


        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { classpath != null? classpath : "" },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);
        CompilationUnit cu = (CompilationUnit) parser.createAST(null);

        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        cu.accept(this);
        return gson.toJson(output);
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
                else if (type.equals("nl")) {
                    String val = ((StringLiteral) value.getValue()).getLiteralValue();
                    output.javadoc = val;
                }
                else if (type.equals("types")) {
                    List<Expression> types = ((ArrayInitializer) value.getValue()).expressions();
                    for (Expression e : types) {
                        String t = ((StringLiteral) e).getLiteralValue();
                        output.types.add(t);
                    }
                }
                else throw new RuntimeException();
            }
        }
        return false;
    }

}
