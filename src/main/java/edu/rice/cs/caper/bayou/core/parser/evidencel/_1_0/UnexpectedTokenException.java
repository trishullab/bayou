package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.Token;

/**
 * Thrown to indicate that a token read during parsing did not conform to the grammar.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public class UnexpectedTokenException extends ParseException
{
    public final Token OffendingToken;

    public UnexpectedTokenException(Token offendingToken)
    {
        OffendingToken = offendingToken;
    }
}
