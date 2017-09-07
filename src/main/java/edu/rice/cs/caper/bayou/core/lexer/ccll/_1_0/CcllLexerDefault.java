package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import edu.rice.cs.caper.bayou.core.lexer.UnexpectedEndOfCharacters;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A simple CcllLexer implementation.
 */
class CcllLexerDefault implements CcllLexer
{
    /**
     * Defines the current state of the lexer. I.e. what type of token is currently under construction.
     */
    private interface LexerState
    {
        /**
         * Informs the state that of the next three characters in the lex stream.
         * Use null for no more characters.
         */
        void newCurrentCharacter(Character current, Character next, Character nextNext);

        /**
         * @return true if the last "current" character shown to newCurrentCharacter(...) completes the current token
         * under construction.
         */
        boolean isTokenCompleted();

        /**
         * The type of the token currently under construction or null if no token is under construction.
         */
        TokenType getTokenType();

    }

    private class LexerStateConstructingLineComment implements LexerState
    {
        private boolean _isTokenCompleted = false;

        @Override
        public void newCurrentCharacter(Character current, Character next, Character nextNext)
        {
            if(current != null && current == '\n') // new line is the last character of a line comment
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

    private class LexerStateConstructingBlockComment implements LexerState
    {
        private boolean _isTokenCompleted = false;

        /**
         * The last value of current provided via newCurrentCharacter(...).
         */
        private Character _prev;

        @Override
        public void newCurrentCharacter(Character current, Character next, Character nextNext)
        {
            if(_prev != null && current != null && _prev == '*'  && current == '/') // */ ends a block comment
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
            return new TokenTypeBlockComment();
        }
    }


    private class LexerStateConstructingOther implements LexerState
    {
        private boolean _isTokenCompleted = false;

        @Override
        public void newCurrentCharacter(Character current, Character next, Character nextNext)
        {
            if(next == null) // other token can be concluded by end of stream
            {
                _isTokenCompleted = true;
            }
            else if(nextNext != null && next == '/' && nextNext == '/') // after current, starting a line comment
            {
                _isTokenCompleted = true;
            }
            else if(nextNext != null && next == '/' && nextNext == '*') // after current, starting a block comment
            {
                _isTokenCompleted = true;
            }
            else if(next == '"') // after current, starting a string
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

    private class LexerStateConstructingString implements LexerState
    {
        private boolean _isTokenCompleted = false;

        /**
         * The last value of current provided via newCurrentCharacter(...).
         */
        private Character _prev;

        @Override
        public void newCurrentCharacter(Character current, Character next, Character nextNext)
        {
            if(_prev != null && current != null && current == '"' && _prev != '\\') // string concluded by "
                _isTokenCompleted = true;                                           // unless escaped by \
                                                                                    // _prev != null also ensure min
            _prev = current;                                                        // string is "" and not "
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

    private class LexerStateEndOfCharacters implements LexerState
    {
        @Override
        public void newCurrentCharacter(Character current, Character next, Character nextNext)
        {
            if(current != null)
                throw new IllegalArgumentException("current may only be null");
        }

        @Override
        public boolean isTokenCompleted()
        {
            return true;
        }

        @Override
        public TokenType getTokenType()
        {
            return null;
        }
    }

    @Override
    public Iterable<Token> lex(Iterable<Character> chars) throws UnexpectedEndOfCharacters
    {
        return lexHelp(chars.iterator());
    }

    private Iterable<Token> lexHelp(Iterator<Character> chars) throws UnexpectedEndOfCharacters
    {
        if(!chars.hasNext())
            return Collections.emptyList();

        LinkedList<Token> tokensToReturn = new LinkedList<>();

        Character current = chars.next();
        int currentIndex = 0; // the index of current in chars starting at 0

        Character next = chars.hasNext() ? chars.next() : null; // the char after current
        Character nextNext = chars.hasNext() ? chars.next() : null; // the char after next

        StringBuilder lexemeAccum = new StringBuilder(); // the accumulated characters of the current token so far
        int lexemeAccumStartIndex = 0; // the index in chars of the first character contributing to the current token

        LexerState state = nextState(current, next); // determine what type of token is under construction

        while(current != null) // while not and end of chars
        {
            lexemeAccum.append(current);
            state.newCurrentCharacter(current, next, nextNext); // inform state of new character from chars

            if(state.isTokenCompleted()) // if current completed the token, create token
            {
                tokensToReturn.add(Token.make(lexemeAccum.toString(), state.getTokenType(), lexemeAccumStartIndex));

                /*
                 * Prepare to start constructing the next token.
                 */
                lexemeAccum = new StringBuilder(); // clear for next token
                lexemeAccumStartIndex = currentIndex + 1; // mark start position of next token
                state = nextState(next, nextNext); // update state of lexer to begin lexing the next token
            }

            /*
             * Advance one character into the chars stream.
             */
            current = next;
            currentIndex++;
            next = nextNext;
            nextNext = chars.hasNext() ? chars.next() : null;

        }

        if(state.getTokenType() != null) // is a token under construction when we ran out of characters?
            throw new UnexpectedEndOfCharacters();

        return tokensToReturn;
    }

    /**
     * Given the next two characters in a stream, determine what type of token is being constructed and return
     * the corresponding state.
     */
    private LexerState nextState(Character current, Character next)
    {
        if(current == null)
            return new LexerStateEndOfCharacters();

        if(current == '"')
            return new LexerStateConstructingString();

        if(current == '/' && next != null && next == '/')
            return new LexerStateConstructingLineComment();

        if(current == '/' && next != null && next == '*')
            return new LexerStateConstructingBlockComment();

        return new LexerStateConstructingOther();
    }

}
