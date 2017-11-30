/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.floodgage.application.floodgage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.ParseException;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import edu.rice.cs.caper.floodgage.application.floodgage.draft_building.DraftBuilder;
import edu.rice.cs.caper.floodgage.application.floodgage.draft_building.DraftBuilderBayou1_1_0;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Directive;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Hole;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.xml.v1.DirectiveXmlV1Parser;
import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.Evidence;
import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.EvidenceCall;
import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.EvidenceKeywords;
import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.EvidenceType;
import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.Trial;
import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.TrialWithSketch;
import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.TrialWithoutSketch;
import edu.rice.cs.caper.floodgage.application.floodgage.synthesizer.Synthesizer;
import edu.rice.cs.caper.floodgage.application.floodgage.view.View;

/**
 * Wrapper for run(...).
 */
class FloodGage
{
    /**
     * Parses "trials.xml" as found as a resource on the current class path and executes the trials against
     * the given Synthesizer reporting progress to the given View.
     *
     * Expects a trialpack to be on the current classpath.
     *
     * @param synthesizer the Synthesizer to use for creating programs from draft programs identified in trials.
     * @param view the UI component for reporting execution progress.
     * @throws IOException if there is a program reading resources (like "trails.xml") on the classpath.
     * @throws ParseException if "trials.xml" is not of the expected format
     * @throws ClassCastException if "PassProgram" or "PassProgramTest" cannot be loaded from teh current classpath.
     */
    void run(Synthesizer synthesizer, View view) throws IOException, ParseException, ClassNotFoundException
    {
        if(synthesizer == null)
            throw new NullPointerException("synthesizer");

        if(view == null)
            throw new NullPointerException("view");

        /*
         * Parse trials.xml from inside the trialpack (assumed to be on classpath) into a Directive structure.
         */
        Directive directive;
        {
            //  We expect the trialpack jar file to be on the classpath.  That jar file should have a
            //  trails.xml file its root directory.  So look for it at just "trails.xml" path.
            String trailsXml = new String(getResource("trials.xml"), StandardCharsets.UTF_8);
            directive = DirectiveXmlV1Parser.makeDefault().parse(trailsXml);
        }

        /*
         * Test the test suite by asserting that all test cases pass when given the pass program.
         *
         * Stop the program if this is not the case.
         */
        // expect PassProgram class inside the trailpack which is assumed to be on the class path
        Class passProgram = getClass().getClassLoader().loadClass("PassProgram");
        // expect PassProgramTest class inside the trailpack which is assumed to be on the class path
        Class passProgramTestSuite = getClass().getClassLoader().loadClass("PassProgramTest");

        view.declareTestingTestSuite(passProgramTestSuite, passProgram);

        Result result = new JUnitCore().run(passProgramTestSuite); // run the test cases against PassProgram
        view.declareNumberOfTestCasesInSuite(result.getRunCount());

        if(result.getFailureCount() > 0) // if the pass program did not pass the test suite
        {
            for (Failure f : result.getFailures())
                view.declarePassProgramTestFailure(f.getTrace());
        }
        else // pass program did pass the test suite, proceed to running the user specified trials
        {
            List<Trial> trials = makeTrials(directive, DraftBuilderBayou1_1_0::new);
            TrialsRunner.runTrails(trials, synthesizer, view);
        }

    }

    /*
     * Convert the trials specified in the given Directive to a list of Trials.  Use a DraftBuilder returned by
     * makeDraftBuilder (one per trial) to rewrite the draft program from the format used inside a trialpack
     * to the format the synthesis system expects as input.
     */
    private List<Trial> makeTrials(Directive directive, Function<String,DraftBuilder> makeDraftBuilder)
            throws IOException
    {
        List<Trial> trialsAccum = new LinkedList<>(); // to be returned

        // example trials.xml for reference

        //<trials>
        //  <trial>
        //    <description>Read string contents of a file.</description>
        //    <draftProgramPath>FileReaderDraft.java</draftProgramPath>
        //    <holes>
        //      <hole id="1">
        //        <evidence type="keywords">read file to a string</evidence>
        //      </hole>
        //    </holes>
        //  </trial>
        //  <trial>
        //    <draftProgramPath>FileReaderDraft.java</draftProgramPath>
        //    <expectedSketchPath>FileReadSketch.java</expectedSketchPath>
        //    <holes>
        //      <hole id="1">
        //        <evidence type="call">readLine</evidence>
        //        <evidence type="type">FileReader</evidence>
        //      </hole>
        //    </holes>
        //  </trial>
        //</trials>

        // build one Trial for every trial specified by the user as found in the directive
        for(edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Trial trial : directive.getTrials())
        {
            /*
             * Retrieve the description of the trial, if any.
             */
            String description = trial.getDescription(); // from <description> in trails.xml (if present)
            description = description == null ? "" : description; // if description not specified, use empty string

            /*
             * Build the draft program source as it should be sent to the synthesizer.  This is a two step process:
             *
             * 1.) Retrieve the "proto" draft program source from the trialpack that uses the "/// #" hole notation.
             *     An example might looks like:
             *
             *     import java.util.function.*;
             *
             *     public class ReadFileLinesDraft implements Consumer<String>
             *     {
             *         public void accept(String filePath)
             *         {
             *             /// 1
             *         }
             *     }
             *
             * 2.) Replace each hole in the "/// #" notation with the hole format that the synthesis system expects
             *     as guided by the hole definition in trials.xml. The returned DraftBuilder from the given
             *     makeDraftBuilder parameter should know how to do this conversion for us.
             *
             *     For example, with Bayou 1.1.0 that would be (with the trials.xml above)
             *
             *     import java.util.function.*;
             *
             *     public class ReadFileLinesDraft implements Consumer<String>
             *     {
             *         public void accept(String filePath)
             *         {
             *             { call:readLine type:FileReader }
             *         }
             *     }
             *
             */
            String draftProgramSource;
            {
                //  We expect the trialpack jar file to be on the classpath.  That jar file should have all draft
                // programs in its root directory.  So look for it at a path that is just the contents of the xml entry.
                String protoDraftProgramSrc = /// using the "// #" notation
                        new String(getResource(trial.getDraftProgramPath()), StandardCharsets.UTF_8);

                DraftBuilder draftBuilder = makeDraftBuilder.apply(protoDraftProgramSrc);

                for(Hole hole : trial.getHoles())
                {
                    // convert evidences from the directive structure to the trial structure
                    List<Evidence> evidence = hole.getEvidence().stream()
                                                .map(this::makeEvidence)
                                                .collect(Collectors.toList());

                    draftBuilder.setHole(hole.getId(), evidence);

                }

                draftProgramSource = draftBuilder.buildDraft();
            }

            /*
             * Retrieve other elements from user defined trial.
             */

            // FileReaderDraft in example above
            String assumeDraftProgramClassName = getFilenameWithoutExtension(trial.getDraftProgramPath());

            String sketchProgramPath = // look for optional sketch entry. null if not present.
                    trial.getSketchProgramPath();

            /*
             * Create Trial instance and append to trialsAccum.
             */
            Trial trialToAppend;
            {
                if (sketchProgramPath != null) // has a sketch specified
                {
                    String sketchProgramSource =
                            new String(getResource(trial.getSketchProgramPath()), StandardCharsets.UTF_8);

                    trialToAppend = TrialWithSketch.make(description, draftProgramSource, assumeDraftProgramClassName,
                            sketchProgramSource);
                }
                else // does not have a sketch specified
                {
                    trialToAppend =
                            TrialWithoutSketch.make(description, draftProgramSource, assumeDraftProgramClassName);
                }
            }

            trialsAccum.add(trialToAppend);
        }

        return trialsAccum;
    }

    /*
     * Convert evidence representations.
     */
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
                throw new IllegalArgumentException("Unknown evidence type: " + evidence.getType());
        }
    }

    /*
     * e.g. "/home/user/Cat.java" -> "Cat"
     */
    private String getFilenameWithoutExtension(String path)
    {
        String[] parts = path.split(File.separator);
        String finalSegment = parts[parts.length-1];

        if(!finalSegment.contains("."))
            return finalSegment;

        return finalSegment.substring(0, finalSegment.lastIndexOf("."));
    }

    /*
     * Gets the byte contents of a resource entry of the current classloader.
     */
    private byte[] getResource(String name) throws IOException
    {
        if(name == null)
            throw new NullPointerException("name");

        ClassLoader loader =  this.getClass().getClassLoader();
        InputStream resourceStream = loader.getResourceAsStream(name);

        /*
         * Read all the bytes provided by resourceStream into a byte buffer and return.
         */
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        {
            int nRead;
            byte[] data = new byte[1024];

            while ((nRead = resourceStream.read(data, 0, data.length)) != -1)
            {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
        }

        return buffer.toByteArray();
    }

}
