package edu.rice.cs.caper.floodgage.application.floodgage.model.directive;

import java.util.Collections;
import java.util.List;

public interface Directive
{
    List<Trial> getTrials();

    static Directive make(List<Trial> trials)
    {
        return new Directive()
        {
            @Override
            public List<Trial> getTrials()
            {
                return Collections.unmodifiableList(trials);
            }
        };
    }


}
