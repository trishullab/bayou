package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

/**
 * A string in the Coarse C-Like Language.  E.g.
 *
 *     "a string"
 */
public class TokenTypeString implements TokenType
{
    @Override
    public <R, T extends Throwable> R match(TokenTypeCases<R,T> cases) throws T
    {
        return cases.forString(this);
    }
}
