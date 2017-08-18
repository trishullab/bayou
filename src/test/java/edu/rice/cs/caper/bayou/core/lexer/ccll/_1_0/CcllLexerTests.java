package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public abstract class CcllLexerTests
{

    protected abstract CcllLexer makeLexer();

    @Test
    public void testLex1()
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("other").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("other", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeOther);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLex2()
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("// line comment").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("// line comment", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeLineComment);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLex3()
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("\" string \"").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("\" string \"", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeOther);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLex4()
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("\" \\\"string \"").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("\" \\\"string \"", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeOther);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLex5()
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("int i; // line comment\n").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("int i; ", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeOther);

        Token second = tokens.next();
        Assert.assertEquals("// line comment\n", second.getLexeme());
        Assert.assertEquals(7, second.getStartIndex());
        Assert.assertTrue(second.getType() instanceof TokenTypeLineComment);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLex6()
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("int i; // line comment\nint j=0;").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("int i; ", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeOther);

        Token second = tokens.next();
        Assert.assertEquals("// line comment\n", second.getLexeme());
        Assert.assertEquals(7, second.getStartIndex());
        Assert.assertTrue(second.getType() instanceof TokenTypeLineComment);

        Token third = tokens.next();
        Assert.assertEquals("int j=0;", third.getLexeme());
        Assert.assertEquals(23, third.getStartIndex());
        Assert.assertTrue(third.getType() instanceof TokenTypeOther);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLex7()
    {

        String program = "import android.bluetooth.BluetoothAdapter;\n" +
                "\n" +
                "// Bayou supports three types of evidence:\n" +
                "// 1. apicalls - API methods the code should invoke\n" +
                "// 2. types - datatypes of objects which invoke API methods\n" +
                "// 3. context - datatypes of variables that the code should use\n" +
                "\n" +
                "public class TestBluetooth {\n" +
                "\n" +
                "    /* Get an input stream that can be used to read from\n" +
                "     * the given blueooth hardware address */\n" +
                "    void readFromBluetooth(BluetoothAdapter adapter) {\n" +
                "        // Intersperse code with evidence\n" +
                "        String address = \"00:43:A8:23:10:F0\";\n" +
                "\n" +
                "        { // Provide evidence within a separate block\n" +
                "            // Code should call \"getInputStream\"...\n" +
                "            /// getInputStream\n" +
                "            // ...on a \"BluetoothSocket\" type\n" +
                "            /// BluetoothSocket\n" +
                "        } // Synthesized code will replace this block\n" +
                "    }   \n" +
                "\n" +
                "}\n";

        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex(program).iterator();

        Assert.assertTrue(tokens.hasNext());

        Token token = tokens.next();
        Assert.assertEquals("import android.bluetooth.BluetoothAdapter;\n\n", token.getLexeme());
        Assert.assertEquals(0, token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("// Bayou supports three types of evidence:\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// Bayou supports"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("// 1. apicalls - API methods the code should invoke\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// 1."), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("// 2. types - datatypes of objects which invoke API methods\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// 2."), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("// 3. context - datatypes of variables that the code should use\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// 3."), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("\npublic class TestBluetooth {\n\n    /* Get an input stream that can be used to read from\n     * the given blueooth hardware address */\n    void readFromBluetooth(BluetoothAdapter adapter) {\n        ", token.getLexeme());
        Assert.assertEquals(program.indexOf("\npublic class TestBluetooth"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("// Intersperse code with evidence\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// Intersperse"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("        String address = ", token.getLexeme());
        Assert.assertEquals(program.indexOf("        String address"), token.getStartIndex());
        Assert.assertEquals(TokenTypeOther.class, token.getType().getClass());



    }
}
