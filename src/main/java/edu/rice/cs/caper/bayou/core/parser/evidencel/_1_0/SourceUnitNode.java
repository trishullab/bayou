package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import java.util.List;

public interface SourceUnitNode
{
    List<EvidenceElement> getElements();

    static SourceUnitNode make(List<EvidenceElement> elements)
    {
        return new SourceUnitNode()
        {
            @Override
            public List<EvidenceElement> getElements()
            {
                return elements;
            }
        };
    }
}
