package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

public interface IdentifierNode
{
    public String getIdentifier();

    static IdentifierNode make(String identifier)
    {
        return new IdentifierNode()
        {
            @Override
            public String getIdentifier()
            {
                return identifier;
            }
        };
    }

}
