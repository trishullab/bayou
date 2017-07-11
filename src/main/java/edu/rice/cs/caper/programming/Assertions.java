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
package edu.rice.cs.caper.programming;

public class Assertions
{
    /**
     * If argumentValue is non-null, returns argumentValue. Otherwise throws a NullPointerException with paramName
     * as the message argument.
     *
     * @param paramName the parameterName that corresponds to argumentValue
     * @param argumentValue the value of the argument
     * @param <T> the type of argumentValue
     * @return argumentValue if argumentValue is non-null
     */
    public static <T> T assertArgumentNonNull(String paramName, T argumentValue)
    {
        if (argumentValue == null)
            throw new NullPointerException(paramName);

        return argumentValue;
    }
}
