package edu.rice.cs.caper.floodgage.application.floodgage.model.directive;

import java.util.Collections;
import java.util.List;

public interface TestSuite
{
    String getTestSuitePath();

    String getPassProgramPath();

    String getResourcePath();

    List<Trial> getTrails();

    static  TestSuite make(String testSuitePath, String passProgramPath, String resourcePath, List<Trial> trails)
    {
        return new TestSuite()
        {
            @Override
            public String getTestSuitePath()
            {
                return testSuitePath;
            }

            @Override
            public String getPassProgramPath()
            {
                return passProgramPath;
            }

            @Override
            public String getResourcePath()
            {
                return resourcePath;
            }

            @Override
            public List<Trial> getTrails()
            {
                return Collections.unmodifiableList(trails);
            }
        };
    }
}
