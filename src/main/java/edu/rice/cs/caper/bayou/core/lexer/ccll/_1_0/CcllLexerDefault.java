package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

public class CcllLexerDefault implements CcllLexer
{
    class LexToken implements Token
    {
        private final String _lexeme;

        private final TokenType _type;

        private final int _index;

        LexToken(StringBuilder builder, TokenType type, int index)
        {
            _lexeme = builder.toString();
            _type = type;
            _index = index;
        }

        @Override
        public String getLexeme()
        {
            return _lexeme;
        }

        @Override
        public TokenType getType()
        {
            return _type;
        }

        @Override
        public int getStartIndex()
        {
            return _index;
        }
    }

    interface LexerState
    {
        void newCurrentCharacter(Character current, Character next, Character nextNext);

        boolean isTokenCompleted();

        TokenType getTokenType();
    }

    class LexerStateConstructingLineComment implements LexerState
    {
        private boolean _isTokenCompleted = false;

        @Override
        public void newCurrentCharacter(Character current, Character next, Character nextNext)
        {
            if(current == '\n')
                _isTokenCompleted = true;
        }

        @Override
        public boolean isTokenCompleted()
        {
            return _isTokenCompleted;
        }

        @Override
        public TokenType getTokenType()
        {
            return new TokenTypeLineComment();
        }
    }

    class LexerStateConstructingOther implements LexerState
    {
        private boolean _isTokenCompleted = false;

        @Override
        public void newCurrentCharacter(Character current, Character next, Character nextNext)
        {
            if(next == null)
            {
                _isTokenCompleted = true;
            }
            else if(next == '/' && nextNext == '/')
            {
                _isTokenCompleted = true;
            }
            else if(next == '"')
            {
                _isTokenCompleted = true;
            }
        }

        @Override
        public boolean isTokenCompleted()
        {
            return _isTokenCompleted;
        }

        @Override
        public TokenType getTokenType()
        {
            return new TokenTypeOther();
        }
    }

    class LexerStateConstructingString implements LexerState
    {
        private boolean _isTokenCompleted = false;

        private Character _prev;

        @Override
        public void newCurrentCharacter(Character current, Character next, Character nextNext)
        {
            if(_prev != null && current == '"' && _prev != '\\')
                _isTokenCompleted = true;

            _prev = current;
        }

        @Override
        public boolean isTokenCompleted()
        {
            return _isTokenCompleted;
        }

        @Override
        public TokenType getTokenType()
        {
            return new TokenTypeString();
        }
    }

    @Override
    public Iterable<Token> lex(Iterable<Character> chars)
    {
        Iterator<Character> charElements = chars.iterator();

        if(!charElements.hasNext())
            return Collections.emptyList();

        LinkedList<Token> tokens = new LinkedList<>();

        Character current = charElements.next();
        int currentIndex = 0;

        Character next = charElements.hasNext() ? charElements.next() : null;
        Character nextNext = charElements.hasNext() ? charElements.next() : null;

        StringBuilder lexemeAccum = new StringBuilder();
        int lexemeAccumStartIndex = 0;

        LexerState state = nextState(current, next);

        while(current != null)
        {
            lexemeAccum.append(current);
            state.newCurrentCharacter(current, next, nextNext);

            if(state.isTokenCompleted())
            {
                tokens.add(new LexToken(lexemeAccum, state.getTokenType(), lexemeAccumStartIndex));
                lexemeAccum = new StringBuilder();
                lexemeAccumStartIndex = currentIndex + 1;
            }

            current = next;
            currentIndex++;
            next = nextNext;
            nextNext = charElements.hasNext() ? charElements.next() : null;

            if(state.isTokenCompleted())
                state = nextState(current, next);
        }

        if(lexemeAccum.length() > 0)
            tokens.add(new LexToken(lexemeAccum, state.getTokenType(), lexemeAccumStartIndex));

        return tokens;

    }

    private LexerState nextState(Character current, Character next)
    {
        if(current == null)
            return new LexerStateConstructingOther();

        if(current == '/' && next == '/')
            return new LexerStateConstructingLineComment();

        if(current == '"')
            return new LexerStateConstructingString();

        return new LexerStateConstructingOther();
    }

}
