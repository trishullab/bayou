package edu.rice.cs.caper.floodgage.application.floodgage;

import edu.rice.cs.caper.floodgage.application.floodgage.view.View;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

/**
 * Wrapper for runTestSuiteAgainst(...)
 */
class TestSuiteRunner
{
    /**
     * Creates and runs a JUnit test runner for testSuite.
     *
     * @param testSuite the class defining test cases to run
     * @param view a UI component to report test failures to
     * @return if all test cases passed
     */
    static boolean runTestSuiteAgainst(Class testSuite, View view)
    {
        if(testSuite == null)
            throw new NullPointerException("testSuite");

        if(view == null)
            throw new NullPointerException("view");

        Result result = new JUnitCore().run(testSuite);

        if (result.getFailureCount() > 0)
        {
            for (Failure f : result.getFailures())
                view.declarePassProgramTestFailure(f.getTrace());

            return false;
        }

        return true;
    }

}
