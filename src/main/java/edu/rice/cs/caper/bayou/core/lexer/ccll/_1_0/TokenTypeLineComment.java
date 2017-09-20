package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

/**
 * A line comment in the Coarse C-Like Language.  E.g.
 *
 *     // a line comment
 */
public class TokenTypeLineComment implements TokenType
{
    @Override
    public <R, T extends Throwable> R match(TokenTypeCases<R,T> cases) throws T
    {
        return cases.forLineComment(this);
    }
}
