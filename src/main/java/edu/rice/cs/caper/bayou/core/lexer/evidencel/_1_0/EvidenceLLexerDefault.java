/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.UnexpectedEndOfCharacters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

public class EvidenceLLexerDefault implements EvidenceLLexer
{
    @Override
    public Iterable<Token> lex(Iterable<Character> chars)
    {
        return lex(chars.iterator());
    }

    private Iterable<Token> lex(Iterator<Character> chars)
    {
        if(!chars.hasNext())
            return Collections.emptyList();

        Character current = chars.next();
        if(current == null)
            throw new IllegalArgumentException("chars may not contain null");

        Character next;
        {
            if(chars.hasNext())
            {
                next = chars.next();
                if(next == null)
                    throw new IllegalArgumentException("chars may not contain null");
            }
            else
            {
                next = null;
            }
        }

        ArrayList<Token> tokensAccum = new ArrayList<>();

        StringBuilder lexemeAccum = new StringBuilder();
        while(current != null)
        {
            if(!Character.isWhitespace(current))
                lexemeAccum.append(current);

            if(next == null || next == ':' || next == ',' || Character.isWhitespace(next))
            {
                String lexeme = lexemeAccum.toString();
                lexemeAccum = new StringBuilder();
                appendTokenIfNotWhitespace(lexeme, tokensAccum);
            }
            else if(current == ':')
            {
                lexemeAccum = new StringBuilder();
                appendTokenIfNotWhitespace(":", tokensAccum);
            }
            else if(current == ',')
            {
                lexemeAccum = new StringBuilder();
                appendTokenIfNotWhitespace(",", tokensAccum);
            }

            current = next;
            if(chars.hasNext())
            {
                next = chars.next();
                if(next == null)
                    throw new IllegalArgumentException("chars may not contain null");
            }
            else
            {
                next = null;
            }
        }

        return tokensAccum;
    }

    private void appendTokenIfNotWhitespace(String lexeme, ArrayList<Token> tokensAccum)
    {
        if(lexeme.trim().length() < 1)
            return;

        switch (lexeme)
        {
            case ":":
                tokensAccum.add(Token.make(":", new TokenTypeColon()));
                break;
            case ",":
                tokensAccum.add(Token.make(",", new TokenTypeComma()));
                break;
            default:
                tokensAccum.add(Token.make(lexeme, new TokenTypeIdentifier()));
                break;
        }
    }


}
