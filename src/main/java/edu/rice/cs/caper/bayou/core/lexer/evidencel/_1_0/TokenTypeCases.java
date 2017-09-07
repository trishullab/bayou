package edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.parser.evidencel._1_0.UnexpectedEndOfTokens;

public interface TokenTypeCases<R,T extends Throwable>
{
    R forIdentifier(TokenTypeIdentifier identifier) throws T;

    R forColon(TokenTypeColon colon) throws T;

    R forComma(TokenTypeComma comma) throws T;
}
