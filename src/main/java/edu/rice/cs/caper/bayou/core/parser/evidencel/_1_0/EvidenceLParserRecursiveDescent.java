package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class EvidenceLParserRecursiveDescent implements EvidenceLParser
{
    @Override
    public SourceUnitNode parse(Iterable<Token> tokens) throws ParseException
    {
        return parse(new TokenStream(tokens.iterator()));
    }

    private SourceUnitNode parse(TokenStream tokens) throws ParseException
    {
        if(tokens.isEmpty())
            return SourceUnitNode.make(Collections.emptyList());

        LinkedList<EvidenceElement> elements = new LinkedList<>();
        while(!tokens.isEmpty())
        {
            EvidenceElement element = makeEvidenceElement(tokens);
            elements.add(element);
        }

        return SourceUnitNode.make(elements);
    }

    private EvidenceElement makeEvidenceElement(TokenStream tokens) throws ParseException
    {
        if(isAtStartOfTypeIdentifier(tokens))
        {
            return makeEvidenceElementWithTypeIdentifier(tokens);
        }
        else
        {
            return makeEvidenceElementWithoutTypeIdentifier(tokens);
        }
    }

    private EvidenceElement makeEvidenceElementWithoutTypeIdentifier(TokenStream tokens) throws ParseException
    {
        IdentifierListNode list = makeIdentifierList(tokens);
        return EvidenceElementWithoutTypeIdentifierNode.make(list);
    }

    private EvidenceElementWithTypeIdentifierNode makeEvidenceElementWithTypeIdentifier(TokenStream tokens) throws ParseException
    {
        TypeIdentifierNode typeIdentifier = makeTypeIdentifier(tokens);
        IdentifierListNode list = makeIdentifierList(tokens);

        return EvidenceElementWithTypeIdentifierNode.make(typeIdentifier, list);

    }

    private IdentifierListNode makeIdentifierList(TokenStream tokens) throws ParseException
    {
        if(tokens.isEmpty())
            throw new UnexpectedEndOfTokens();

        ArrayList<IdentifierNode> idents = new ArrayList<>();
        idents.add(makeIdentifierNode(tokens));

        while(!tokens.isEmpty() && isComma(tokens.peek()))
        {
            tokens.pop();
            idents.add(makeIdentifierNode(tokens));
        }

        return IdentifierListNode.make(idents);

    }

    private boolean isComma(Token token)
    {
        return token.getType() instanceof TokenTypeComma;
    }

    private IdentifierNode makeIdentifierNode(TokenStream tokens) throws UnexpectedEndOfTokens, UnexpectedTokenException
    {
        if(tokens.isEmpty())
            throw new UnexpectedEndOfTokens();

        Token token = tokens.pop();

        return token.getType().match(new TokenTypeCases<IdentifierNode, UnexpectedTokenException>()
        {
            @Override
            public IdentifierNode forIdentifier(TokenTypeIdentifier identifier)
            {
                return IdentifierNode.make(token.getLexeme());
            }

            @Override
            public IdentifierNode forColon(TokenTypeColon colon) throws UnexpectedTokenException
            {
                throw new UnexpectedTokenException(token);
            }

            @Override
            public IdentifierNode forComma(TokenTypeComma comma) throws UnexpectedTokenException
            {
                throw new UnexpectedTokenException(token);
            }
        });
    }

    private TypeIdentifierNode makeTypeIdentifier(TokenStream tokens) throws UnexpectedEndOfTokens, UnexpectedTokenException
    {
        if(tokens.isEmpty())
            throw new UnexpectedEndOfTokens();

        Token first = tokens.pop();

        return first.getType().match(new TokenTypeCases<TypeIdentifierNode, UnexpectedTokenException>()
        {
            @Override
            public TypeIdentifierNode forIdentifier(TokenTypeIdentifier identifier) throws UnexpectedTokenException
            {
                Token second = tokens.pop();
                return second.getType().match(new TokenTypeCases<TypeIdentifierNode, UnexpectedTokenException>()
                {
                    @Override
                    public TypeIdentifierNode forIdentifier(TokenTypeIdentifier identifier) throws UnexpectedTokenException
                    {
                        throw new UnexpectedTokenException(second);
                    }

                    @Override
                    public TypeIdentifierNode forColon(TokenTypeColon colon)
                    {
                        return TypeIdentifierNode.make(first.getLexeme());
                    }

                    @Override
                    public TypeIdentifierNode forComma(TokenTypeComma comma) throws UnexpectedTokenException
                    {
                        throw new UnexpectedTokenException(second);
                    }
                });
            }

            @Override
            public TypeIdentifierNode forColon(TokenTypeColon colon) throws UnexpectedTokenException
            {
                throw new UnexpectedTokenException(first);
            }

            @Override
            public TypeIdentifierNode forComma(TokenTypeComma comma)  throws UnexpectedTokenException
            {
                throw new UnexpectedTokenException(first);
            }
        });
    }

    private boolean isAtStartOfTypeIdentifier(TokenStream tokens)
    {
        return tokens.hasNext() &&
               tokens.lookAhead().getType().match(new TokenTypeCases<Boolean, RuntimeException>()
        {
            @Override
            public Boolean forIdentifier(TokenTypeIdentifier identifier) throws RuntimeException
            {
                return false;
            }

            @Override
            public Boolean forColon(TokenTypeColon colon) throws RuntimeException
            {
                return true;
            }

            @Override
            public Boolean forComma(TokenTypeComma comma) throws RuntimeException
            {
                return false;
            }
        });
    }

}
