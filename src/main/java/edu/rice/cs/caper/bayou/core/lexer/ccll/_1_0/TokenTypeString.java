package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

public class TokenTypeString implements TokenType
{
    @Override
    public <R> R match(TokenTypeCases<R> cases)
    {
        return cases.forString(this);
    }
}
