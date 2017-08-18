package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

public interface Token
{
    String getLexeme();

    TokenType getType();

    int getStartIndex();
}
