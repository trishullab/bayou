package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A simple CcllLexer implementation.
 */
public class CcllLexerDefault implements CcllLexer
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
         * The type of the token currently under construction.
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
            if(_prev != null && current != null && _prev == '*'  && current == '/') // */ end a line comment
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

    @Override
    public Iterable<Token> lex(Iterable<Character> chars) throws UnexpectedEndOfCharacters
    {
        Iterator<Character> charElements = chars.iterator();

        if(!charElements.hasNext())
            return Collections.emptyList();

        return lexHelp(charElements);
    }

    private Iterable<Token> lexHelp(Iterator<Character> chars) throws UnexpectedEndOfCharacters
    {
        if(!chars.hasNext())
            throw new IllegalArgumentException("chars must be non-empty");

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

        state.newCurrentCharacter(null, null, null); // inform state of end of stream.

        /*
         * The only token type that can be legally concluded by end of stream is Other, so assert this is the case.
         *
         * Restated no non-Other token can not legally be under construction when EOF is reached.
         */
        state.getTokenType().match(new TokenTypeCases<Void, UnexpectedEndOfCharacters>()
        {
            @Override
            public Void forLineComment(TokenTypeLineComment lineComment) throws UnexpectedEndOfCharacters
            {
                throw new UnexpectedEndOfCharacters(); // didn't finish line comment by EOF
            }

            @Override
            public Void forOther(TokenTypeOther other)
            {
                return null; // EOF can terminate an Other token
            }

            @Override
            public Void forString(TokenTypeString string) throws UnexpectedEndOfCharacters
            {
                throw new UnexpectedEndOfCharacters(); // didn't finish string by EOF
            }

            @Override
            public Void forBlockComment(TokenTypeBlockComment blockComment)  throws UnexpectedEndOfCharacters
            {
                throw new UnexpectedEndOfCharacters(); // didn't finish block comment by EOF
            }
        });

        // at this point we have certified an other token is under construction. However it may or may not be an
        // "empty" token depending on whether the last character in the stream actually concluded a non-other token
        // for example
        //
        //    "an entire one line program"
        //
        // will have state be LexerStateConstructingOther at this point even though the entire program is a single
        // string token.  In this case lexemeAccum will be empty
        //
        // On the other hand
        //
        //   "token 1" token2
        //
        // will have state be LexerStateConstructingOther at this point but lexemeAccum will be non-empty.
        // we need to put that token in the accumulation to return.
        if(lexemeAccum.length() > 0)
            tokensToReturn.add(Token.make(lexemeAccum.toString(), state.getTokenType(), lexemeAccumStartIndex));

        return tokensToReturn;
    }

    /**
     * Given the next two characters in a stream, determine what type of token is being constructed and return
     * the corresponding state.
     */
    private LexerState nextState(Character current, Character next)
    {
        if(current == null)
            return new LexerStateConstructingOther(); // an other token may be of length 0

        if(current == '/' && next == '/')
            return new LexerStateConstructingLineComment();

        if(current == '/' && next == '*')
            return new LexerStateConstructingBlockComment();

        if(current == '"')
            return new LexerStateConstructingString();

        return new LexerStateConstructingOther();
    }

}
