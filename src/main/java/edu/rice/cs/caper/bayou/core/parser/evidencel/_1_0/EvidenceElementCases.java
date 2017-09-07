package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

public interface EvidenceElementCases<R, T extends Throwable>
{
    R forWithoutTypeIdent(EvidenceElementWithoutTypeIdentifierNode evidenceElement) throws T;

    R forWithTypeIdent(EvidenceElementWithTypeIdentifierNode evidenceElement) throws T;
}
