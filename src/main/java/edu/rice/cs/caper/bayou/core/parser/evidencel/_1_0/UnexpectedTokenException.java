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
package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.Token;

/**
 * Thrown to indicate that a token read during parsing did not conform to the grammar.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public class UnexpectedTokenException extends ParseException
{
    public final Token OffendingToken;

    public UnexpectedTokenException(Token offendingToken)
    {
        super("Unexpected token " + offendingToken.getLexeme());
        OffendingToken = offendingToken;
    }
}
