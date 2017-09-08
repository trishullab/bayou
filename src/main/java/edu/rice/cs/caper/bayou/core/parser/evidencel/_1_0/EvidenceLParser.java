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
