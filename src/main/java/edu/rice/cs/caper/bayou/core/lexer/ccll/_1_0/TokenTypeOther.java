package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

/**
 * An "Other" token Coarse C-Like Language.  E.g. not a block comment, line comment, or string. E.g.
 *
 *     int i = 0;
 */
public class TokenTypeOther implements TokenType
{
    @Override
    public <R, T extends Throwable> R match(TokenTypeCases<R, T> cases) throws T
    {
        return cases.forOther(this);
    }
}
