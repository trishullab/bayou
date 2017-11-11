package edu.rice.cs.caper.floodgage.application.floodgage.model.directive;

import java.util.Collections;
import java.util.List;

public interface Trial
{
    String getDescription();

    String getDraftProgramPath();

    String getSketchProgramPath();

    List<Hole> getHoles();

    static Trial make(String description, String draftProgramPath, String sketchProgramPath, List<Hole> holes)
    {
        return new Trial()
        {
            @Override
            public String getDescription()
            {
                return description;
            }

            @Override
            public String getDraftProgramPath()
            {
                return draftProgramPath;
            }

            @Override
            public String getSketchProgramPath()
            {
                return sketchProgramPath;
            }

            @Override
            public List<Hole> getHoles()
            {
                return Collections.unmodifiableList(holes);
            }
        };
    }
}
