/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
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
