package edu.rice.cs.caper.floodgage.application.floodgage;

import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.TestSuite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

class SourceResolver
{
    class TestSuiteSourceResolution
    {
        final String TesSuiteSource;

        final String PassProgramSource;

        TestSuiteSourceResolution(String tesSuiteSource, String passProgramSource)
        {
            TesSuiteSource = tesSuiteSource;
            PassProgramSource = passProgramSource;
        }
    }

    TestSuiteSourceResolution resolve(Path principleDirectory, TestSuite testSuite) throws IOException
    {
        Path testSuitePath = principleDirectory.resolve(testSuite.getTestSuitePath());
        String testSuitePathSource = new String(Files.readAllBytes(testSuitePath));

        Path passProgramPath = principleDirectory.resolve(testSuite.getPassProgramPath());
        String passProgramSource = new String(Files.readAllBytes(passProgramPath));

        return new TestSuiteSourceResolution(testSuitePathSource, passProgramSource);
    }
}
