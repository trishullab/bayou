package edu.rice.cs.caper.floodgage.application.floodgage.model.directive;

import java.util.Collections;
import java.util.List;

public interface Hole
{
    String getId();

    List<Evidence> getEvidence();

    static Hole make(String id, List<Evidence> evidence)
    {
        return new Hole()
        {
            @Override
            public String getId()
            {
                return id;
            }

            @Override
            public List<Evidence> getEvidence()
            {
                return Collections.unmodifiableList(evidence);
            }
        };
    }
}
