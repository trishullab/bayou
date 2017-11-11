package edu.rice.cs.caper.floodgage.application.floodgage;

import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.Trial;
import edu.rice.cs.caper.floodgage.application.floodgage.synthesizer.SynthesizeException;
import edu.rice.cs.caper.floodgage.application.floodgage.synthesizer.Synthesizer;
import edu.rice.cs.caper.floodgage.application.floodgage.view.View;
import edu.rice.cs.caper.bayou.core.dom_driver.Visitor;
import edu.rice.cs.caper.bayou.core.dom_driver.Options;
import net.openhft.compiler.CompilerUtils;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

class TrailsRunner
{
    static void runTrails(List<Trial> trails, Synthesizer synthesizer, View view)
    {
        int trailId = 0;
        for(Trial trial : trails)
        {
            trailId++;

            String draftProgram = trial.getDraftProgramSource();
            view.declareStartOfTrial(trial.getDescription(), draftProgram);

            List<String> synthResults;
            try
            {
                synthResults = synthesizer.synthesize(draftProgram);
            }
            catch (SynthesizeException e)
            {
                view.declareSynthesisFailed();
               continue;
            }


            String assumedResultClassName = trial.getDraftProgramClassName();


            view.declareNumberOfResults(synthResults.size());

            int resultId = 0;
            for(String result : synthResults)
            {
                resultId++;
                String uniqueResultClassName = assumedResultClassName + "_" + trailId + "_" + resultId;
                result = result.replaceFirst("public class " + assumedResultClassName, "public class " + uniqueResultClassName);

                view.declareSynthesizeResult(resultId, result);

                try
                {
                    CompilerUtils.CACHED_COMPILER.loadFromJava(uniqueResultClassName, result);
                }
                catch (ClassNotFoundException e)
                {
                    view.declareSyntResultDoesNotCompile(resultId);
                    continue;
                }

                Class resultSpecificTestSuite = makeTrialSpecificTestSuite(uniqueResultClassName);

                view.declareTestingResult(resultId);
                TestSuiteRunner.runTestSuiteAgainst(resultSpecificTestSuite, view);

                if(trial.containsSketchProgramSource())
                {
                    String expectedSketchSource = trial.tryGetSketchProgramSource(null);


                    if(expectedSketchSource == null)
                        throw new RuntimeException("Expected access to succeed after containsSketchProgramSource() check");

                    String expectedSketch = makeSketchFromSource(expectedSketchSource, trial.getDraftProgramClassName() + ".java");
                    String resultSketch = makeSketchFromSource(result, uniqueResultClassName + ".java");
                    int j = 0;

                }
            }

        }
    }

    private static String makeSketchFromSource(String source, String unitName)
    {
        CompilationUnit cu;
        {
            ASTParser parser = ASTParser.newParser(AST.JLS8);

            parser.setSource(source.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setUnitName(unitName);
            parser.setEnvironment(new String[] { "" }, new String[] { "" }, new String[] { "UTF-8" }, true);
            parser.setResolveBindings(true);

            cu = (CompilationUnit) parser.createAST(null);
        }
        Options options = new Options();
        String sketch;
        try
        {
            Visitor visitor = new Visitor(cu, options);
            cu.accept(visitor);
            return visitor.buildJson();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static Class makeTrialSpecificTestSuite(String assumedResultClassName)
    {
        String className =  assumedResultClassName + "Test";
        String source = "public class " + className + " extends TestSuite"  + "\n" +
                "{\n" +
                "\t@Override\n" +
                "\tprotected " + assumedResultClassName  + " makeTestable()" + "\n" +
                "\t{\n" +
                "\t\treturn new " + assumedResultClassName + "();\n" +
                "\t}\n" +
                "}";

        try
        {
            return CompilerUtils.CACHED_COMPILER.loadFromJava(className, source);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }
}
