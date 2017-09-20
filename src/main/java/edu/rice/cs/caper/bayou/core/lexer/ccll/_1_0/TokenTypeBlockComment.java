package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

/**
 * A block comment in the Coarse C-Like Language.
 */
public class TokenTypeBlockComment  implements TokenType
{
    @Override
    public <R,T extends Throwable> R match(TokenTypeCases<R,T> cases) throws T
    {
        return cases.forBlockComment(this);
    }
}
