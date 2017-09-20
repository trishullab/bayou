package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

/**
 * Models the evidence-element-with-type-identifier non-terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public interface EvidenceElementWithTypeIdentifierNode extends EvidenceElement
{
    /**
     * @return the type identifier part of the evidence.
     */
    TypeIdentifierNode getTypeIdentifier();

    @Override
    default <R, T extends Throwable> R match(EvidenceElementCases<R, T> cases) throws T
    {
        return cases.forWithTypeIdent(this);
    }

    /**
     * Creates a EvidenceElementWithTypeIdentifierNode with the given type identifier and ident list.
     *
     * @param typeIdentifier the type identifier part of the of the EvidenceElementWithoutTypeIdentifierNode
     * @param list the identifier list part of the EvidenceElementWithoutTypeIdentifierNode
     * @return  a new EvidenceElementWithTypeIdentifierNode instance
     */
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
