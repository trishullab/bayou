package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

public interface EvidenceElementWithoutTypeIdentifierNode extends EvidenceElement
{
    IdentifierListNode getIdentifierList();

    default <R, T extends Throwable> R match(EvidenceElementCases<R, T> cases) throws T
    {
        return cases.forWithoutTypeIdent(this);
    }

    static EvidenceElementWithoutTypeIdentifierNode make(IdentifierListNode list)
    {
        return new EvidenceElementWithoutTypeIdentifierNode()
        {

            @Override
            public IdentifierListNode getIdentifierList()
            {
                return list;
            }
        };
    }
}
