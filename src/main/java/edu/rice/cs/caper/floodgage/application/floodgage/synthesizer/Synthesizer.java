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
package edu.rice.cs.caper.floodgage.application.floodgage.synthesizer;

import java.util.List;

/**
 * A component that can synthesize programs from a given program draft.
 */
public interface Synthesizer
{
    /**
     * Synthesizes programs from the given draft. The format of draftProgram is sub-type specific.
     *
     * @param draftProgram a draft program
     * @return completed programs based on draftProgram
     * @throws SynthesizeException if synthesis can not be completed.
     */
    List<String> synthesize(String draftProgram) throws SynthesizeException;
}
