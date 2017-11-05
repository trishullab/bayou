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
package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

import edu.rice.cs.caper.bayou.core.lexer.UnexpectedEndOfCharacters;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public abstract class CcllLexerTests
{

    protected abstract CcllLexer makeLexer();

    @Test
    public void testLexEmpty() throws UnexpectedEndOfCharacters
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("").iterator();
        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLexOther() throws UnexpectedEndOfCharacters
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
    public void testLexLineComment()  throws UnexpectedEndOfCharacters
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("// line comment\n").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("// line comment\n", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeLineComment);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLexBlockComment()  throws UnexpectedEndOfCharacters
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("/* block comment */").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("/* block comment */", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeBlockComment);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLexString1() throws UnexpectedEndOfCharacters
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("\" string \"").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("\" string \"", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeString);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test
    public void testLexString2() throws UnexpectedEndOfCharacters
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("\" \\\"string \"").iterator();

        Assert.assertTrue(tokens.hasNext());

        Token first = tokens.next();
        Assert.assertEquals("\" \\\"string \"", first.getLexeme());
        Assert.assertEquals(0, first.getStartIndex());
        Assert.assertTrue(first.getType() instanceof TokenTypeString);

        Assert.assertFalse(tokens.hasNext());
    }

    @Test(expected = UnexpectedEndOfCharacters.class)
    public void testLexStringUnterminated() throws UnexpectedEndOfCharacters
    {
        CcllLexer lexer = makeLexer();

        Iterator<Token> tokens = lexer.lex("\" forget to close").iterator();
    }

    @Test
    public void testLexOtherLineComment() throws UnexpectedEndOfCharacters
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
    public void testLexOtherLineCommentOther() throws UnexpectedEndOfCharacters
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
    public void testLexTestBluetooth() throws UnexpectedEndOfCharacters
    {

        String program = "import android.bluetooth.BluetoothAdapter;\n" +
                "\n" +
                "// Bayou supports three types of evidence:\n" +
                "// 1. apicalls - API methods the code should invoke\n" +
                "// 2. types - datatypes of objects which invoke API methods\n" +
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
        Assert.assertEquals("\npublic class TestBluetooth {\n\n    ", token.getLexeme());
        Assert.assertEquals(program.indexOf("\npublic class TestBluetooth"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("/* Get an input stream that can be used to read from\n     * the given blueooth hardware address */", token.getLexeme());
        Assert.assertEquals(program.indexOf("/* Get an input stream"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeBlockComment);

        token = tokens.next();
        Assert.assertEquals("\n    void readFromBluetooth(BluetoothAdapter adapter) {\n        ", token.getLexeme());
        Assert.assertEquals(program.indexOf("\n    void readFromBluetooth"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("// Intersperse code with evidence\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// Intersperse"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("        String address = ", token.getLexeme());
        Assert.assertEquals(program.indexOf("        String address"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("\"00:43:A8:23:10:F0\"", token.getLexeme());
        Assert.assertEquals(program.indexOf("\"00:43:A8:23:10:F0\""), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeString);

        token = tokens.next();
        Assert.assertEquals(";\n\n        { ", token.getLexeme());
        Assert.assertEquals(program.indexOf(";\n\n        { "), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("// Provide evidence within a separate block\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// Provide evidence"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("            ", token.getLexeme());
        Assert.assertEquals(program.indexOf("            // Code should"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("// Code should call \"getInputStream\"...\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// Code should call"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("            ", token.getLexeme());
        Assert.assertEquals(program.indexOf("            /// getInputStream\n"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("/// getInputStream\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("/// getInputStream\n"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("            ", token.getLexeme());
        Assert.assertEquals(program.indexOf("            // ...on a \"BluetoothSocket\" type\n"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("// ...on a \"BluetoothSocket\" type\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// ...on a \"BluetoothSocket\" type\n"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("            ", token.getLexeme());
        Assert.assertEquals(program.indexOf("            /// BluetoothSocket\n"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("/// BluetoothSocket\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("/// BluetoothSocket\n"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("        } ", token.getLexeme());
        Assert.assertEquals(program.indexOf("        } "), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeOther);

        token = tokens.next();
        Assert.assertEquals("// Synthesized code will replace this block\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("// Synthesized code will replace this block\n"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeLineComment);

        token = tokens.next();
        Assert.assertEquals("    }   \n\n}\n", token.getLexeme());
        Assert.assertEquals(program.indexOf("    }   \n\n}\n"), token.getStartIndex());
        Assert.assertTrue(token.getType() instanceof  TokenTypeOther);


    }
}
