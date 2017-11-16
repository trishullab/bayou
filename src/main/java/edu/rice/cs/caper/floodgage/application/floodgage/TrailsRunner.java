package edu.rice.cs.caper.floodgage.application.floodgage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.core.dsl.*;
import edu.rice.cs.caper.bayou.core.sketch_metric.EqualityASTMetric;
import edu.rice.cs.caper.bayou.core.synthesizer.RuntimeTypeAdapterFactory;
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
import org.json.JSONObject;
import org.junit.experimental.theories.DataPoint;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

class TrailsRunner
{
    static void runTrails(List<Trial> trails, Synthesizer synthesizer, View view)
    {
        int trailId = 0;
        int numPasses = 0;
        int numFailures = 0;
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
                numFailures++;
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
                    view.declareResult(false);
                    numFailures++;
                    continue;
                }

                Class resultSpecificTestSuite = makeTrialSpecificTestSuite(uniqueResultClassName);

                view.declareTestingResult(resultId);
                boolean passSoFar = TestSuiteRunner.runTestSuiteAgainst(resultSpecificTestSuite, view);

                if(trial.containsSketchProgramSource())
                {
                    String expectedSketchSource = trial.tryGetSketchProgramSource(null);

                    if(expectedSketchSource == null)
                        throw new RuntimeException("Expected access to succeed after containsSketchProgramSource() check");

                    DSubTree expectedSketch = makeSketchFromSource(expectedSketchSource, trial.getDraftProgramClassName() + ".java");
                    DSubTree resultSketch = makeSketchFromSource(result, uniqueResultClassName + ".java");


                    if(expectedSketch == null && resultSketch == null)
                    {
                        view.warnSketchesNull();
                    }
                    if(expectedSketch != null && resultSketch != null)
                    {
                        float equalityResult = new EqualityASTMetric().compute(expectedSketch, Arrays.asList(resultSketch), "");
                        view.declareSketchMetricResult(equalityResult);
                        if(equalityResult != 1)
                        {
                            passSoFar = false;
                        }
                    }
                    else
                    {
                        view.warnSketchesNullMismatch();
                        passSoFar = false;
                    }
                }

                if(passSoFar)
                    numPasses++;
                else
                    numFailures++;
                view.declareResult(passSoFar);

            }

        }

        view.declareTally(numPasses, numFailures);
    }


    private static DSubTree makeSketchFromSource(String source, String unitName)
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
            sketch = visitor.buildJson();
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        if(sketch == null)
            return null;

        JSONObject sketchObject = new JSONObject(sketch);
        JSONObject program = sketchObject.getJSONArray("programs").getJSONObject(0);
        JSONObject astNode = program.getJSONObject("ast");

        RuntimeTypeAdapterFactory<DASTNode> nodeAdapter = RuntimeTypeAdapterFactory.of(DASTNode.class, "node")
                .registerSubtype(DAPICall.class)
                .registerSubtype(DBranch.class)
                .registerSubtype(DExcept.class)
                .registerSubtype(DLoop.class)
                .registerSubtype(DSubTree.class);
        Gson gson = new GsonBuilder().serializeNulls()
                .registerTypeAdapterFactory(nodeAdapter)
                .create();
        return gson.fromJson(astNode.toString(), DSubTree.class);

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
