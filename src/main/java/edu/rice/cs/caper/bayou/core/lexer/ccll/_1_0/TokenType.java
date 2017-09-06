package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

public interface TokenType
{
    <R, T extends Throwable> R match(TokenTypeCases<R,T> cases) throws T;
}
