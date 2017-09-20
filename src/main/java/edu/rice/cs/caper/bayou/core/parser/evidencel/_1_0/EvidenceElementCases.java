package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

/**
 * The two cases of EvidenceElement for the Visitor Pattern.
 */
public interface EvidenceElementCases<R, T extends Throwable>
{
    R forWithoutTypeIdent(EvidenceElementWithoutTypeIdentifierNode evidenceElement) throws T;

    R forWithTypeIdent(EvidenceElementWithTypeIdentifierNode evidenceElement) throws T;
}
