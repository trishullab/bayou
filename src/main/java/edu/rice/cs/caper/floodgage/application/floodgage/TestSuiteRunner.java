package edu.rice.cs.caper.floodgage.application.floodgage;

import edu.rice.cs.caper.floodgage.application.floodgage.model.plan.TestSuite;
import edu.rice.cs.caper.floodgage.application.floodgage.view.View;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.spec.ECField;

class TestSuiteRunner
{
    static boolean runTestSuiteAgainst(Class testSuite, View view)
    {
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
