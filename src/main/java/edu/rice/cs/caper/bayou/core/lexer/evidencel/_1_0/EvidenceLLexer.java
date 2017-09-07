package edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0;


import java.util.Iterator;

public interface EvidenceLLexer
{
    /**
     * Lexes the given character sequence into tokens.
     */
    Iterable<Token> lex(Iterable<Character> chars);

    /**
     * Lexes the given character sequence into tokens.
     */
    default Iterable<Token> lex(CharSequence sequence)
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

    /**
     * @return some implementation of EvidenceLLexer.
     */
    static EvidenceLLexer makeDefault()
    {
        return new EvidenceLLexerDefault();
    }
}
