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

public interface View
{

    void declareTestingTestSuite(Class testSuite, Class passProgram);

    void declareStartOfTrial(String description, String draftProgramSource);

    void declareSynthesizeResult(Object id, String result);

    void declareTrialResultPassProgramDidntPass();

    void declareTrialResultSynthesisFailed();

    void declareTrialResultResultsDidntCompile();

    void declareSynthResultResult(boolean compiled, boolean testCasesPass, Boolean matchesSketch);

    void declareSyntResultDoesNotCompile(Object resultId);

    void declareSynthesisFailed();

    void declareNumberOfResults(int size);

    void declarePassProgramTestFailure(String message);

    void declareNumberOfTestCasesInSuite(int runCount);

    void warnSketchesNull();

    void declareSketchMetricResult(float equalityResult);

    void warnSketchesNullMismatch();

    void declarePointScore(int points, int possiblePoints);
}
