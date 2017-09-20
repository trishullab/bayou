package edu.rice.cs.caper.bayou.core.lexer;

/**
 * Thrown during lexing to indicate that the provided characters were exhausted before token construction was completed
 * for a specific token.
 *
 * E.g.
 *
 *     "A string that was started but never terminated
 */
public class UnexpectedEndOfCharacters extends Exception
{
}
