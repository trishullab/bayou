package edu.rice.cs.caper.bayou.core.lexer.ccll._1_0;

/**
 * The different types of tokens in Coarse C-Like Language.
 *
 * Used in Visitor Pattern in conjunction with Token.
 */
public interface TokenTypeCases<R, T extends Throwable>
{
    R forLineComment(TokenTypeLineComment lineComment) throws T;

    R forOther(TokenTypeOther other) throws T;

    R forString(TokenTypeString string) throws T;

    R forBlockComment(TokenTypeBlockComment blockComment) throws T;
}
