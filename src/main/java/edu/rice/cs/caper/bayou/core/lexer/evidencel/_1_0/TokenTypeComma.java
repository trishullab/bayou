package edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0;

public class TokenTypeComma
        implements TokenType
{
    @Override
    public <R, T extends Throwable> R match(TokenTypeCases<R,T> cases) throws T
    {
        return cases.forComma(this);
    }
}
