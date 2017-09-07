package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface IdentifierListNode
{
    List<IdentifierNode> getIdentifiers();

    static IdentifierListNode make(ArrayList<IdentifierNode> idents)
    {
        return new IdentifierListNode()
        {
            @Override
            public List<IdentifierNode> getIdentifiers()
            {
                return Collections.unmodifiableList(idents);
            }
        };
    }
}
