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
package edu.rice.cs.caper.floodgage.application.floodgage.view;

public class ViewConsole implements View
{
    private int _trailNumber = 0;


    @Override
    public void declareTestingTestSuite(Class testSuite, Class passProgram)
    {
        System.out.print("\n[Testing " + testSuite.getName() + " using " + passProgram.getName() + "]");
    }

    @Override
    public void declareStartOfTrial(String description, String draftProgramSource)
    {
        _trailNumber++;

        System.out.println();
        System.out.println();
        System.out.println("==============================================");
        System.out.println("Trial " + _trailNumber);
        System.out.println(description);
        System.out.println("==============================================");

        System.out.println("\n[Draft]\n");

        for(String line : draftProgramSource.split("\n"))
        {
            System.out.println("\t" + line);
        }


    }

    @Override
    public void declareSynthesizeResult(Object resultId, String result)
    {
        System.out.println("\n\n[Synthesis Result " + resultId + "]\n");

        for(String line : result.split("\n"))
        {
            System.out.println("\t" + line);
        }
    }

    @Override
    public void declareTrialResultPassProgramDidntPass()
    {
        System.out.print("\n[FAIL] (Pass program didn't pass.)");
    }

    @Override
    public void declareStartOfTestCases()
    {
        System.out.print("\n[Starting to run test runner.]");
    }

    @Override
    public void declareTrialResultSynthesisFailed()
    {
        System.out.print("\n[FAIL] (Synthesis failed.)");
    }


    @Override
    public void declareSynthResultResult(boolean compiled, boolean testCasesPass, Boolean matchesSketch)
    {
        if(!compiled && testCasesPass)
            throw new IllegalArgumentException("Cannot run tests against uncompiled program");

        boolean sketchFailed = matchesSketch != null && !matchesSketch;

        if(compiled && testCasesPass && !sketchFailed)
        {
            System.out.print("\n[PASS]");
        }
        else
        {
            System.out.print("\n[FAIL]");

            if(!compiled)
                System.out.print("\n\tResult did not compile.");

            if(!testCasesPass)
                System.out.print("\n\tTest cases did not pass.");

            if(sketchFailed)
                System.out.print("\n\tNo sketches matched.");
        }


    }


    @Override
    public void declareSynthesisFailed()
    {
        System.out.print("\n !!! Synthesis Failed !!!");
    }

    @Override
    public void declareNumberOfResults(int size)
    {
        System.out.print("\n " + size + " programs synthesized.");
    }

    @Override
    public void declarePassProgramTestFailure(String message)
    {
        System.out.println();
        System.out.println();
        for(String line : message.split("\n"))
        {
            System.out.println("\t" + line);
        }
    }

    @Override
    public void declareNumberOfTestCasesInSuite(int numTestCases)
    {
        System.out.print("\n"  + numTestCases + " test cases found.");
    }

    @Override
    public void warnSketchesNull()
    {
        System.out.print("\n[Warning] Sketches are equal, but both are null.");
    }

    @Override
    public void declareSketchMetricResult(float equalityResult)
    {
        System.out.print("[Sketch match value is " + equalityResult + "]");
    }

    @Override
    public void warnSketchesNullMismatch()
    {
        System.out.print("\n[Warning] Only one sketch is null.");
    }

    @Override
    public void declarePointScore(int possibleCompilePointsAccum, int obtainedCompilePointsAccum,
                                  int possibleTestCasePointsAccum, int obtainedTestCasePointsAccum,
                                  int possibleSketchMatchPointsAccum, int obtainedSketchMatchPointsAccum)
    {
        if(possibleCompilePointsAccum < 0)
            throw new IllegalArgumentException("possibleCompilePointsAccum must be >=0");

        if(obtainedCompilePointsAccum < 0)
            throw new IllegalArgumentException("obtainedCompilePointsAccum must be >=0");

        if(obtainedCompilePointsAccum > possibleCompilePointsAccum)
            throw new IllegalArgumentException("obtainedCompilePointsAccum may not excede possibleCompilePointsAccum");


        if(possibleTestCasePointsAccum < 0)
            throw new IllegalArgumentException("possibleTestCasePointsAccum must be >=0");

        if(obtainedTestCasePointsAccum < 0)
            throw new IllegalArgumentException("obtainedTestCasePointsAccum must be >=0");

        if(obtainedTestCasePointsAccum > possibleTestCasePointsAccum)
            throw new IllegalArgumentException("obtainedTestCasePointsAccum may not excede possibleTestCasePointsAccum");


        if(possibleSketchMatchPointsAccum < 0)
            throw new IllegalArgumentException("possibleSketchMatchPointsAccum must be >=0");

        if(obtainedSketchMatchPointsAccum < 0)
            throw new IllegalArgumentException("obtainedSketchMatchPointsAccum must be >=0");

        if(obtainedSketchMatchPointsAccum > possibleSketchMatchPointsAccum)
            throw new IllegalArgumentException("obtainedSketchMatchPointsAccum may not excede possibleSketchMatchPointsAccum");


        System.out.println("\n==============================================");
        System.out.println("\nCompile Score: " + obtainedCompilePointsAccum + " / " + possibleCompilePointsAccum);
        System.out.println("\nTest Case Score: " + obtainedTestCasePointsAccum + " / " + possibleTestCasePointsAccum);

        if(possibleSketchMatchPointsAccum > 0)
            System.out.println("\nSketch Match Score: " + obtainedSketchMatchPointsAccum + " / " + possibleSketchMatchPointsAccum);

    }

}
