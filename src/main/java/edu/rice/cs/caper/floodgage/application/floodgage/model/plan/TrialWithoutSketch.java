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
package edu.rice.cs.caper.floodgage.application.floodgage.model.plan;

/**
 * A Trial that does not include a sketch.
 */
public interface TrialWithoutSketch extends Trial
{
    /**
     * Constructs a TrialWithoutSketch with the given values.
     *
     * @param description a textual description of the trial. (May be the empty string for no description.)
     * @param draftProgramSource the source code of a class (the "draft program") used in synthesis.
     * @param draftProgramClassName the name of the class defined by getDraftProgramSource().
     * @return a TrialWithoutSketch instance of unspecified implementation.
     */
    static TrialWithoutSketch make(String description, String draftProgramSource, String draftProgramClassName)
    {
        if(description == null)
            throw new NullPointerException("description");

        if(draftProgramSource == null)
            throw new NullPointerException("draftProgramSource");

        if(draftProgramClassName == null)
            throw new NullPointerException("draftProgramClassName");

        return new TrialWithoutSketch()
        {
            @Override
            public String getDescription()
            {
                return description;
            }

            @Override
            public String getDraftProgramSource()
            {
                return draftProgramSource;
            }

            @Override
            public String getDraftProgramClassName()
            {
                return draftProgramClassName;
            }

            @Override
            public boolean containsSketchProgramSource()
            {
                return false;
            }

            @Override
            public String tryGetSketchProgramSource(String failValue)
            {
                return failValue;
            }
        };
    }
}
