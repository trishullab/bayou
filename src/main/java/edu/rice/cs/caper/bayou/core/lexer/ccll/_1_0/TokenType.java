package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

public interface TokenType
{
    <R> R match(TokenTypeCases<R> cases);
}
