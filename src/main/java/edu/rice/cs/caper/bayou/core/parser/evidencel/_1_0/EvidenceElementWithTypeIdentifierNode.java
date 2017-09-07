package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

public interface EvidenceElementWithTypeIdentifierNode extends EvidenceElement
{
    TypeIdentifierNode getTypeIdentifier();

    @Override
    default <R, T extends Throwable> R match(EvidenceElementCases<R, T> cases) throws T
    {
        return cases.forWithTypeIdent(this);
    }

    static EvidenceElementWithTypeIdentifierNode make(TypeIdentifierNode typeIdentifier, IdentifierListNode list)
    {
        return new EvidenceElementWithTypeIdentifierNode()
        {

            @Override
            public TypeIdentifierNode getTypeIdentifier()
            {
                return typeIdentifier;
            }

            @Override
            public IdentifierListNode getIdentifierList()
            {
                return list;
            }
        };
    }
}
