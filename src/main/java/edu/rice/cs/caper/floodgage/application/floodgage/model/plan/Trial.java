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
 * Models a trial to be evaluated by Floodgage.
 */
public interface Trial
{
    /**
     * @return a textual description of the trial. (May be the empty string for no description.)
     */
    String getDescription();

    /**
     * @return the source code of a class (the "draft program") used in synthesis.
     */
    String getDraftProgramSource();

    /**
     * @return the name of the class defined by getDraftProgramSource().
     */
    String getDraftProgramClassName();

    /**
     * @return if the trial contains a sketch program.
     */
    boolean containsSketchProgramSource();

    /**
     * If the trial contains a sketch program, returns the source of that program. Otherwise returns failValue.
     *
     * @param failValue the value to return if the trial does not have a sketch program.
     * @return  If the trial contains a sketch program, returns the source of that program. Otherwise returns failValue.
     */
    String tryGetSketchProgramSource(String failValue);
}
