package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

/**
 * Models the evidence-element non-terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public interface EvidenceElement
{
    IdentifierListNode getIdentifierList();

    <R, T extends Throwable> R match(EvidenceElementCases<R,T> cases) throws T;
}
