package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

public interface TypeIdentifierNode
{
    String getIdentifier();

    static TypeIdentifierNode make(String identifier)
    {
        return new TypeIdentifierNode()
        {
            @Override
            public String getIdentifier()
            {
                return identifier;
            }
        };
    }
}
