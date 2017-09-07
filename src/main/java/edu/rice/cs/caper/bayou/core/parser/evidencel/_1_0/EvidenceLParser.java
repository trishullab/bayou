package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.EvidenceLLexer;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.Token;

public interface EvidenceLParser
{
    default SourceUnitNode parse(String evidence) throws ParseException
    {
        return parse(EvidenceLLexer.makeDefault().lex(evidence));
    }

    SourceUnitNode parse(Iterable<Token> tokens) throws ParseException;

    static EvidenceLParser makeDefault()
    {
        return new EvidenceLParserRecursiveDescent();
    }
}
