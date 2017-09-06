package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

/**
 * A fragment of a program in the Coarse C-Like Language.
 */
public interface Token
{
    /**
     * @return the contents of the token
     */
    String getLexeme();

    /**
     * @return the type of the token
     */
    TokenType getType();

    /**
     * @return the zero based index of the first character of the token in the original program unit.
     */
    int getStartIndex();

    /**
     * Creates a Token.
     *
     * @param lexeme the contents of the token
     * @param type the type of the token
     * @param startIndex the zero based index of the first character of the token in the original program
     *                   unit.
     * @return the token
     */
    static Token make(String lexeme, TokenType type, int startIndex)
    {
        return new Token()
        {
            @Override
            public String getLexeme()
            {
                return lexeme;
            }

            @Override
            public TokenType getType()
            {
                return type;
            }

            @Override
            public int getStartIndex()
            {
                return startIndex;
            }
        };
    }
}
