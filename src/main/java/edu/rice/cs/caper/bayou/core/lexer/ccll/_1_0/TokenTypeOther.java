package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

public class TokenTypeOther implements TokenType
{
    @Override
    public <R> R match(TokenTypeCases<R> cases)
    {
        return cases.forOther(this);
    }
}
