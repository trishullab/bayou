package edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0;

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
     * Creates a Token.
     *
     * @param lexeme the contents of the token
     * @param type the type of the token
     * @return the token
     */
    static Token make(String lexeme, TokenType type)
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

        };
    }
}
