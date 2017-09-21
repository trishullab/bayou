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

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.EvidenceLLexer;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.Token;

/**
 * A parser of the EvidenceL language.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public interface EvidenceLParser
{
    /**
     * Parses the given string into a parse tree.
     *
     * @param evidence the string to parse
     * @return the root of the parse tree
     * @throws ParseException if the given string does not conform to the EvidenceL grammar.
     */
    default SourceUnitNode parse(String evidence) throws ParseException
    {
        return parse(EvidenceLLexer.makeDefault().lex(evidence));
    }

    /**
     * Parses the given tokens into a parse tree.
     *
     * @param tokens the tokens to parse
     * @return the root of the parse tree
     * @throws ParseException if the given string does not conform to the EvidenceL grammar.
     */
    SourceUnitNode parse(Iterable<Token> tokens) throws ParseException;

    /**
     * @return an instance of a default EvidenceLParser implementation.
     */
    static EvidenceLParser makeDefault()
    {
        return new EvidenceLParserRecursiveDescent();
    }
}
