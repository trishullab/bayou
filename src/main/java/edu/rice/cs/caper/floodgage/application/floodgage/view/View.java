package edu.rice.cs.caper.floodgage.application.floodgage.view;

public interface View
{
    void declareStartOfCompilingTestSuites();

    void declareCompiling(String path);

    void declareCompilationComplete(String path);


    void declareInvoking(String name);

    void declareTestingTestSuite(Class testSuite, Class passProgram);

    void declareStartOfTrial(String description, String draftProgramSource);

    void declareSynthesizeResult(Object id, String result);

    void declareTestingResult(Object resultId);

    void declareResult(boolean success);

    void declareClassDoesNotInstantiate(String classname);

    void declareSyntResultDoesNotCompile(Object resultId);

    void declareSynthesisFailed();

    void declareNumberOfResults(int size);

    void declarePassProgramTestFailure(String message);

    void declareNumberOfTestCasesInSuite(int runCount);

    void warnSketchesNull();

    void declareSketchMetricResult(float equalityResult);

    void warnSketchesNullMismatch();

    void declareTally(int numPasses, int numFailures);
}
