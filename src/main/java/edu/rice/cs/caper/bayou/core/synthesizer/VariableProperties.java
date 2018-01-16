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
 * Properties of a variable in the type system. Allows chaining method calls.
 */
public class VariableProperties {

    /**
     * Denotes if the variable is a user-defined variable in the param/body of the method
     */
    private boolean userVar;

    /**
     * Denotes if the variable should participate in joins (e.g., catch clause variables will not)
     */
    private boolean join;

    /**
     * Denotes if the variable needs to be initialized to a default value using $init
     */
    private boolean defaultInit;

    /**
     * Sets the userVar property
     * @param b value to set to
     * @return this object for chaining
     */
    public VariableProperties setUserVar(boolean b) {
        userVar = b;
        return this;
    }

    /**
     * Sets the join property
     * @param b value to set to
     * @return this object for chaining
     */
    public VariableProperties setJoin(boolean b) {
        join = b;
        return this;
    }

    /**
     * Sets the defaultInit property
     * @param b value to set to
     * @return this object for chaining
     */
    public VariableProperties setDefaultInit(boolean b) {
        defaultInit = b;
        return this;
    }

    /**
     * Gets the userVar property
     * @return current value
     */
    public boolean getUserVar() {
        return userVar;
    }

    /**
     * Gets the join property
     * @return current value
     */
    public boolean getJoin() {
        return join;
    }

    /**
     * Gets the defaultInit property
     * @return current value
     */
    public boolean getDefaultInit() {
        return defaultInit;
    }
}
