package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.Token;

import java.util.Iterator;

/**
 * A sequence of tokens.
 */
class TokenStream
{
    /**
     *  Underlying token source.
     */
    private final Iterator<Token> _tokens;

    /**
     * The next token in the stream or null if the stream is exhausted.
     */
    private Token _head;

    /**
     * The token after _head or null if _head is the last token of the stream.
     */
    private Token _next;

    /**
     * @param tokens the tokens of the stream.  No element of tokens may be null.
     */
    TokenStream(Iterator<Token> tokens)
    {
        _tokens = tokens;

        if(!tokens.hasNext())
        {
            _head = null;
            _next = null;
        }
        else
        {
            _next = tokens.next();
            pop(); // moves _next to _head and reads the next token to _next
        }
    }

    /**
     * @return returns and removes the next token in the stream
     * @throws IllegalStateException if the stream is exhausted
     * @throws IllegalStateException if a token proved to the stream during construction is null
     */
    Token pop()
    {
        if(_head == null)
            throw new IllegalStateException();

        Token toReturn = _head;
        _head = _next;

        if(!_tokens.hasNext())
        {
            _next = null;
        }
        else
        {
            _next = _tokens.next();
            if(_next == null)
                throw new IllegalStateException("_tokens may not contain null");
        }

        return toReturn;
    }

    /**
     * @return true if the stream is exhausted of tokens.
     */
    boolean isEmpty()
    {
        return _head == null;
    }

    /**
     * @return true if the stream has at least two tokens remaining.
     */
    boolean hasNext()
    {
        return _next != null;
    }

    /**
     * @return the next token of the stream without removing it from the stream.
     * @throws IllegalStateException if the stream is exhausted
     */
    Token peek()
    {
        if(_head == null)
            throw new IllegalStateException();

        return _head;
    }

    /**
     * @return the token after next of the stream without removing any tokens from the stream.
     * @throws IllegalStateException if fewer than two tokens remain in the stream
     */
    Token lookAhead()
    {
        if(_next == null)
            throw new IllegalStateException();

        return _next;
    }
}
