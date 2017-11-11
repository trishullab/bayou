package edu.rice.cs.caper.floodgage.application.floodgage;

import edu.rice.cs.caper.floodgage.application.floodgage.draft_building.DraftBuilder;
import edu.rice.cs.caper.floodgage.application.floodgage.draft_building.DraftBuilderBayou1_1_0;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Directive;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Hole;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.xml.v1.DirectiveXmlV1Parser;
import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.Evidence;
import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.EvidenceCall;
import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.EvidenceKeywords;
import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.EvidenceType;
import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.TestSuite;
import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.Trial;
import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.TrialWithSketch;
import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.TrialWithoutSketch;
import edu.rice.cs.caper.floodgage.application.floodgage.synthesizer.EchoSynthesizer;
import edu.rice.cs.caper.floodgage.application.floodgage.synthesizer.Synthesizer;
import edu.rice.cs.caper.floodgage.application.floodgage.synthesizer.SynthesizerBayou_1_1_0;
import edu.rice.cs.caper.floodgage.application.floodgage.view.View;
import net.openhft.compiler.CompilerUtils;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class FloodGage
{
    void run(Synthesizer synthesizer, View view) throws Exception
    {
        String trailsXml = new String(getResource("trials.xml"));

        Directive directive = DirectiveXmlV1Parser.makeDefault().parse(trailsXml);

        Class passProgram = getClass().getClassLoader().loadClass("PassProgram");
        Class testSuite = getClass().getClassLoader().loadClass("PassProgramTest");

        view.declareTestingTestSuite(testSuite, passProgram);

        Result result = new JUnitCore().run(testSuite);

        if(result.getFailureCount() > 0)
        {
            view.declareResult(false);

            for (Failure f : result.getFailures())
                view.declarePassProgramTestFailure(f.getTrace());
        }

        if(result.getFailureCount() > 0)
            return;

        view.declareResult(true);

        view.declareNumberOfTestCasesInSuite(result.getRunCount());

        List<Trial> trails = makeTrials(directive, this::makeDefaultDraftBuilder, view);

        TrailsRunner.runTrails(trails, synthesizer, view);

    }


    private DraftBuilder makeDefaultDraftBuilder(String protoDraft)
    {
        return new DraftBuilderBayou1_1_0(protoDraft);
    }



    private List<Trial> makeTrials(Directive directive, Function<String,DraftBuilder> makeDraftBuilder,
                                   View view) throws IOException
    {
        List<Trial> trials = new LinkedList<>();

        for(edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Trial trial : directive.getTrials())
        {
            String description = trial.getDescription();
            description = description == null ? "" : description;

            String protoDraftProgramSource = new String(getResource(trial.getDraftProgramPath()));

            DraftBuilder draftBuilder = makeDraftBuilder.apply(protoDraftProgramSource);

            for(Hole hole : trial.getHoles())
            {
                List<Evidence> evidence = hole.getEvidence()
                        .stream().map(this::makeEvidence).collect(Collectors.toList());
                draftBuilder.setHole(hole.getId(), evidence);

            }

            trial.getSketchProgramPath();

            String assumeDraftProgramClassName = getFilenameWithoutExtension(trial.getDraftProgramPath());

            String sketchProgramPath = trial.getSketchProgramPath();

            if(sketchProgramPath != null)
            {
                String sketchProgramSource = new String(getResource(trial.getSketchProgramPath()));
                trials.add(TrialWithSketch.make(description, draftBuilder.buildDraft(), assumeDraftProgramClassName,
                                                sketchProgramSource));
            }
            else
            {
                trials.add(TrialWithoutSketch.make(description, draftBuilder.buildDraft(),
                           assumeDraftProgramClassName));
            }


        }

        return trials;
    }

    private Evidence makeEvidence(edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Evidence evidence)
    {
        switch (evidence.getType())
        {
            case "keywords":
                return new EvidenceKeywords(evidence.getContent());
            case "call":
                return new EvidenceCall(evidence.getContent());
            case "type":
                return new EvidenceType(evidence.getContent());
            default:
                throw new IllegalArgumentException("");
        }
    }

    private String getFilenameWithoutExtension(String path)
    {
        String finalSegment = path.split(File.separator)[0];
        return finalSegment.substring(0, finalSegment.lastIndexOf("."));
    }

    private byte[] getResource(String name) throws IOException
    {
        ClassLoader loader =  getClass().getClassLoader();
        InputStream resourceStream = loader.getResourceAsStream(name);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[1024];

        while ((nRead = resourceStream.read(data, 0, data.length)) != -1)
        {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();

        return buffer.toByteArray();
    }


}
