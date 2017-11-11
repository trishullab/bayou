package edu.rice.cs.caper.floodgage.application.floodgage.model.plan;

import java.nio.file.Path;

public class TestSuite
{
    public final Class Class;

    public final Class PassProgram;

    public final Path ResourcePath;

    public TestSuite(Class clazz, Class passProgram, Path resourcePath)
    {
        Class = clazz;
        PassProgram = passProgram;
        ResourcePath = resourcePath;
    }
}
