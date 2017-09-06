package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import java.util.Iterator;

/**
 * A lexer for version 1.0 of the Coarse C-Like Language.
 */
public interface CcllLexer
{
    /**
     * Lexes the given character sequence into tokens.
     */
    Iterable<Token> lex(Iterable<Character> chars) throws UnexpectedEndOfCharacters;

    /**
     * Lexes the given character sequence into tokens.
     */
    default Iterable<Token> lex(CharSequence sequence) throws UnexpectedEndOfCharacters
    {
        Iterable<Character> iterable = () -> new Iterator<Character>()
        {
            int _i = 0;

            @Override
            public boolean hasNext()
            {
                return _i < sequence.length();
            }

            @Override
            public Character next()
            {
                return sequence.charAt(_i++);
            }
        };

        return lex(iterable);
    }
}
