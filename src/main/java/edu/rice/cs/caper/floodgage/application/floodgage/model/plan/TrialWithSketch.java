package edu.rice.cs.caper.floodgage.application.floodgage.model.plan;

public interface TrialWithSketch extends Trial
{
    static TrialWithSketch make(String description, String draftProgramSource, String draftProgramClassName,
                                String sketchProgramSource)
    {
        return new TrialWithSketch()
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
                return true;
            }

            @Override
            public String tryGetSketchProgramSource(String failValue)
            {
                return sketchProgramSource;
            }

        };
    }
}
