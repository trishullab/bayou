package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

/**
 * Models the evidence-element-without-type-identifier non-terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public interface EvidenceElementWithoutTypeIdentifierNode extends EvidenceElement
{
    @Override
    default <R, T extends Throwable> R match(EvidenceElementCases<R, T> cases) throws T
    {
        return cases.forWithoutTypeIdent(this);
    }

    /**
     * Creates a EvidenceElementWithoutTypeIdentifierNode with the given ident list.
     *
     * @param list the identifier list of the EvidenceElementWithoutTypeIdentifierNode
     * @return a new EvidenceElementWithoutTypeIdentifierNode instance
     */
    static EvidenceElementWithoutTypeIdentifierNode make(IdentifierListNode list)
    {
        //noinspection Convert2Lambda reduces readability
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
