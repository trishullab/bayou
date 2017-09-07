package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.Token;

import java.util.Iterator;

class TokenStream
{
    private final Iterator<Token> _tokens;

    private Token _head;

    private Token _next;

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
            pop();
        }
    }

    Token pop()
    {
        Token current = _head;
        _head = _next;
        if(!_tokens.hasNext())
        {
            _next = null;
        }
        else
        {
            _next = _tokens.next();
            if(_next == null)
            {
                throw new IllegalStateException("_tokens may not contain null");
            }
        }

        return current;
    }

    boolean isEmpty()
    {
        return _head == null;
    }

    boolean hasNext()
    {
        return _next != null;
    }

    Token peek()
    {
        if(_head == null)
            throw new IllegalStateException();

        return _head;
    }

    Token lookAhead()
    {
        if(_next == null)
            throw new IllegalStateException();

        return _next;
    }
}
