package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

/**
 * Models the identifier terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public interface IdentifierNode
{
    /**
     * @return the identifier
     */
    String getIdentifier();

    /**
     * Creates a new IdentifierNode instance with the given identifier.
     *
     * @param identifier the identifier
     * @return a new IdentifierNode instance
     */
    static IdentifierNode make(String identifier)
    {
        //noinspection Convert2Lambda reduces readability
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
