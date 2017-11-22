package edu.rice.cs.caper.floodgage.application.floodgage.view;

public class ViewConsole implements View
{
    private int _trailNumber = 0;

    @Override
    public void declareStartOfCompilingTestSuites()
    {
        System.out.println();
        System.out.println("=====================");
        System.out.println("Compiling Test Suites");
        System.out.println("=====================");
    }

    @Override
    public void declareCompiling(String path)
    {
        System.out.print("Compiling " + path + "...");
    }

    @Override
    public void declareCompilationComplete(String path)
    {
        System.out.println("ok.");
    }


    @Override
    public void declareInvoking(String name)
    {
        System.out.print("\n\n\tInvoking " + name);
    }

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
    public void declareTestingResult(Object resultId)
    {
        System.out.print("\n\n[Testing Synthesis Result " + resultId + "]");
    }

    @Override
    public void declareTrialResult(boolean success)
    {
        if(success)
            System.out.print("\n[PASS]");
        else
            System.out.print("\n[FAIL]");
    }

    @Override
    public void declareClassDoesNotInstantiate(String classname)
    {
        System.out.print("\n" + classname + " does not instantiate.");
    }

    @Override
    public void declareSyntResultDoesNotCompile(Object resultId)
    {
        System.out.print("\n" + resultId + " does not compile. [FAIL]");
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
        System.out.print("\n[Warning] Only one sketch is null");
    }

    @Override
    public void declareTally(int numPasses, int numFailures)
    {
        System.out.println("\n==============================================");
        System.out.println(numPasses + " passes and " + numFailures + " failures.");
    }

}
