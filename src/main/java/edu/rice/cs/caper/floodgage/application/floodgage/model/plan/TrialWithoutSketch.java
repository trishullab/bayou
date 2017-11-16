package edu.rice.cs.caper.floodgage.application.floodgage.model.plan;

public interface TrialWithoutSketch extends Trial
{
    static TrialWithoutSketch make(String description, String draftProgramSource, String draftProgramClassName)
    {
        return new TrialWithoutSketch()
        {
            @Override
            public String getDescription()
            {
                return description;
            }

            @Override
            public String getDraftProgramSource()
            {
                return draftProgramSource;
            }

            @Override
            public String getDraftProgramClassName()
            {
                return draftProgramClassName;
            }

            @Override
            public boolean containsSketchProgramSource()
            {
                return false;
            }

            @Override
            public String tryGetSketchProgramSource(String failValue)
            {
                return failValue;
            }
        };
    }
}
