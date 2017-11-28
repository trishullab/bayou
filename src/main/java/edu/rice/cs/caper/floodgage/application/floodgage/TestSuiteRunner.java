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
