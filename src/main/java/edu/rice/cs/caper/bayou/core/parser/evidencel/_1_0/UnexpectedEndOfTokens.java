package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

/**
 * Thrown to indicate that additional tokens were expected during parsing.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public class UnexpectedEndOfTokens extends ParseException
{
    public UnexpectedEndOfTokens()
    {
        super("Unexpected end of tokens.");
    }
}
