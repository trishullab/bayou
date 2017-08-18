package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

public interface TokenTypeCases<R>
{
    R forLineComment(TokenTypeLineComment lineComment);

    R forOther(TokenTypeOther other);

    R forString(TokenTypeString string);
}
