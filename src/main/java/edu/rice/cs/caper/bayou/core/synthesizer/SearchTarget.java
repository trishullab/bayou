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
package edu.rice.cs.caper.bayou.core.synthesizer;

/**
 * A thin wrapper for searching for an expression in the enumerator
 */
public class SearchTarget {

    /**
     * The type to search for
     */
    private final Type type;

    /**
     * The parameter name (if any) for which the search is being conducted.
     * Used in order to create meaningful variable names if the search failed.
     */
    private final String name;

    /**
     * Initializes the search target type and parameter name
     * @param type type to search for
     * @param name parameter name for search
     */
    public SearchTarget(Type type, String name) {
        this.type = type;
        this.name = name;
    }

    /**
     * Initializes the search target type
     * @param type type to search for
     */
    public SearchTarget(Type type) {
        this.type = type;
        this.name = null;
    }

    /**
     * Checks if the search target has a parameter name
     * @return if name is not null
     */
    public boolean hasName() {
        return name != null;
    }

    /**
     * Gets the type for the search target
     * @return current value
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets the name for the search target (if any)
     * @return current value
     */
    public String getName() {
        return name;
    }

}
