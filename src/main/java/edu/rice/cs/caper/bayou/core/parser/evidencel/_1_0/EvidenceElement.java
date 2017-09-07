package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

public interface EvidenceElement
{
    IdentifierListNode getIdentifierList();

    <R, T extends Throwable> R match(EvidenceElementCases<R,T> cases) throws T, T;
}
