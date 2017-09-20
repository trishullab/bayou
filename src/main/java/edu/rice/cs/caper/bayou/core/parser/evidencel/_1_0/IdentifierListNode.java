package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Models the identifier-list non-terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public interface IdentifierListNode
{
    /**
     * @return the elements of the list
     */
    List<IdentifierNode> getIdentifiers();

    /**
     * Creates an IdentifierListNode with the given type identifier and ident list.
     *
     * @param idents the identifiers of the list
     * @return a new IdentifierListNode instance
     */
    static IdentifierListNode make(ArrayList<IdentifierNode> idents)
    {
        //noinspection Convert2Lambda reduces readability
        return new IdentifierListNode()
        {
            @Override
            public List<IdentifierNode> getIdentifiers()
            {
                return Collections.unmodifiableList(idents);
            }
        };
    }
}
