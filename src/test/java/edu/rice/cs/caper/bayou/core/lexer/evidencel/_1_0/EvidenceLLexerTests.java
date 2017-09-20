package edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.EvidenceLLexer;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.Token;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.TokenTypeColon;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.TokenTypeIdentifier;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public abstract class EvidenceLLexerTests
{
    abstract EvidenceLLexer makeLexer();

    @Test
    public void testLexEmpty()
    {
        EvidenceLLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("").iterator();

        Assert.assertFalse(tokens.hasNext());

    }

    @Test
    public void testLexColon()
    {
        EvidenceLLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex(":").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token token = tokens.next();
        Assert.assertEquals(":", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeColon);
    }

    @Test
    public void testLexIdent()
    {
        EvidenceLLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("ident").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token token = tokens.next();
        Assert.assertEquals("ident", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);
    }

    @Test
    public void testLexIdents()
    {
        EvidenceLLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("ident1 ident2").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token token = tokens.next();
        Assert.assertEquals("ident1", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);

        token = tokens.next();
        Assert.assertEquals("ident2", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);
    }

    @Test
    public void testLexAlternating()
    {
        EvidenceLLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("ident1:ident2:ident3").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token token = tokens.next();
        Assert.assertEquals("ident1", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);

        token = tokens.next();
        Assert.assertEquals(":", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeColon);

        token = tokens.next();
        Assert.assertEquals("ident2", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);

        token = tokens.next();
        Assert.assertEquals(":", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeColon);

        token = tokens.next();
        Assert.assertEquals("ident3", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);

    }

    @Test
    public void testLexMixed1()
    {
        EvidenceLLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("ident1: ident2, ident3").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token token = tokens.next();
        Assert.assertEquals("ident1", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);

        token = tokens.next();
        Assert.assertEquals(":", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeColon);

        token = tokens.next();
        Assert.assertEquals("ident2", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);

        token = tokens.next();
        Assert.assertEquals(",", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeComma);

        token = tokens.next();
        Assert.assertEquals("ident3", token.getLexeme());
        Assert.assertTrue(token.getType() instanceof TokenTypeIdentifier);

    }
}
