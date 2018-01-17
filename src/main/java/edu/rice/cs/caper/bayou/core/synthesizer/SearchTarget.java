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
 * A wrapper for searching for a target type in the enumerator.
 * Also contains fields useful for making a decision when the search passes/fails.
 */
public class SearchTarget {

    /**
     * The type to search for -- the only required field for SearchTarget
     */
    private final Type type;

    /**
     * The parameter name (if any) for which the search is being conducted.
     * Used in order to create meaningful variable names if the search failed.
     */
    private String paramName;

    /**
     * Denotes whether the enumerator should create a single use variable or not
     * if the search failed (e.g., API call arguments)
     */
    private boolean singleUseVariable;

    /**
     * The name of the API call whose argument is being searched for.
     * Used for cost metric to order variables if the search passed.
     */
    private String apiCallName;

    /**
     * Initializes the search target type
     *
     * @param type type to search for
     */
    public SearchTarget(Type type) {
        this.type = type;
    }

    /**
     * Gets the type for the search target
     *
     * @return current value
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the parameter name for the search
     *
     * @param s the name
     * @return this object for chaining
     */
    public SearchTarget setParamName(String s) {
        paramName = s;
        return this;
    }

    /**
     * Gets the name for the search target (if any)
     *
     * @return current value
     */
    public String getParamName() {
        return paramName;
    }

    /**
     * Sets the value of singleUseVariable search property
     *
     * @param b value to set to
     * @return this object for chaining
     */
    public SearchTarget setSingleUseVariable(boolean b) {
        singleUseVariable = b;
        return this;
    }

    /**
     * Gets the value of singleUseVariable search property
     *
     * @return current value
     */
    public boolean getSingleUseVariable() {
        return singleUseVariable;
    }

    /**
     * Sets the API call name whose argument is being searched for
     *
     * @param s the API call name
     * @return this object for chaining
     */
    public SearchTarget setAPICallName(String s) {
        apiCallName = s;
        return this;
    }

    /**
     * Gets the API call name whose argument is being searched for
     *
     * @return the API call name
     */
    public String getAPICallName() {
        return apiCallName;
    }
}
