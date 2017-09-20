package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * A recursive descent implementation of EvidenceLParser.
 */
public class EvidenceLParserRecursiveDescent implements EvidenceLParser
{
    @Override
    public SourceUnitNode parse(Iterable<Token> tokens) throws ParseException
    {
        return parse(new TokenStream(tokens.iterator()));
    }

    // consumes tokens from tokens to create a SourceUnitNode
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

    // consumes tokens from tokens to create a EvidenceElement
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

    // consumes tokens from tokens to create an EvidenceElementWithoutTypeIdentifierNode
    private EvidenceElementWithoutTypeIdentifierNode makeEvidenceElementWithoutTypeIdentifier(TokenStream tokens)
            throws ParseException
    {
        IdentifierListNode list = makeIdentifierList(tokens);
        return EvidenceElementWithoutTypeIdentifierNode.make(list);
    }

    // consumes tokens from tokens to create an EvidenceElementWithTypeIdentifierNode
    private EvidenceElementWithTypeIdentifierNode makeEvidenceElementWithTypeIdentifier(TokenStream tokens)
            throws ParseException
    {
        TypeIdentifierNode typeIdentifier = makeTypeIdentifier(tokens);
        IdentifierListNode list = makeIdentifierList(tokens);

        return EvidenceElementWithTypeIdentifierNode.make(typeIdentifier, list);

    }

    // consumes tokens from tokens to create an IdentifierListNode
    private IdentifierListNode makeIdentifierList(TokenStream tokens) throws ParseException
    {
        if(tokens.isEmpty())
            throw new UnexpectedEndOfTokens();

        ArrayList<IdentifierNode> idents = new ArrayList<>();
        idents.add(makeIdentifierNode(tokens));

        while(!tokens.isEmpty() && tokens.peek().getType() instanceof TokenTypeComma)
        {
            tokens.pop();
            idents.add(makeIdentifierNode(tokens));
        }

        return IdentifierListNode.make(idents);

    }

    // consumes tokens from tokens to create an IdentifierNode
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

    // consumes tokens from tokens to create a TypeIdentifierNode
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
                    public TypeIdentifierNode forIdentifier(TokenTypeIdentifier identifier)
                            throws UnexpectedTokenException
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

    // tests whether at least two tokens remain and the next two are TokenTypeIdentifier TokenTypeColon
    private boolean isAtStartOfTypeIdentifier(TokenStream tokens)
    {
        if(!tokens.hasNext()) // need two tokens to make a type ident
            return  false;

        return tokens.peek().getType().match(new TokenTypeCases<Boolean, RuntimeException>()
        {
            @Override
            public Boolean forIdentifier(TokenTypeIdentifier identifier)
            {
                return tokens.lookAhead().getType().match(new TokenTypeCases<Boolean, RuntimeException>()
                        {
                            @Override
                            public Boolean forIdentifier(TokenTypeIdentifier identifier)
                            {
                                return false;
                            }

                            @Override
                            public Boolean forColon(TokenTypeColon colon)
                            {
                                return true;
                            }

                            @Override
                            public Boolean forComma(TokenTypeComma comma)
                            {
                                return false;
                            }
                        });
            }

            @Override
            public Boolean forColon(TokenTypeColon colon)
            {
                return false;
            }

            @Override
            public Boolean forComma(TokenTypeComma comma)
            {
                return false;
            }
        });
    }

}
