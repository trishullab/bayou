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

/**
 * Models the identifier terminal.
 *
 * See doc/internal/evidencel_language/1_0/evidencel_language_1_0_grammar.txt
 */
public interface IdentifierNode
{
    /**
     * @return the identifier
     */
    String getIdentifier();

    /**
     * Creates a new IdentifierNode instance with the given identifier.
     *
     * @param identifier the identifier
     * @return a new IdentifierNode instance
     */
    static IdentifierNode make(String identifier)
    {
        //noinspection Convert2Lambda reduces readability
        return new IdentifierNode()
        {
            @Override
            public String getIdentifier()
            {
                return identifier;
            }
        };
    }

}
