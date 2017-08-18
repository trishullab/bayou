package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;


import java.util.Iterator;

public interface CcllLexer
{
    Iterable<Token> lex(Iterable<Character> chars);

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
}
