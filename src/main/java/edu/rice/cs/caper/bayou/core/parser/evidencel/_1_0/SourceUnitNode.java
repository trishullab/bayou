package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import java.util.Collections;
import java.util.List;

/**
 * Models the source-unit non-terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public interface SourceUnitNode
{
    /**
     * @return the elements of the source unit.
     */
    List<EvidenceElement> getElements();

    /**
     * Creates a new SourceUnitNode containing the given elements.
     *
     * @param elements the elements
     * @return a new SourceUnitNode
     */
    static SourceUnitNode make(List<EvidenceElement> elements)
    {
        //noinspection Convert2Lambda reduces readability
        return new SourceUnitNode()
        {
            @Override
            public List<EvidenceElement> getElements()
            {
                return Collections.unmodifiableList(elements);
            }
        };
    }
}
