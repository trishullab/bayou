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
package edu.rice.cs.caper.floodgage.application.floodgage.draft_building;

import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.Evidence;

import java.util.List;

/**
 * A builder that associates ids with evidences and then, upon invocation of buildDraft(), creates a source code string
 * placing evidence at each associated id location in a DraftBuilder specific way.
 */
public interface DraftBuilder
{
    /**
     * Associates the given evidence with the given id.
     *
     * @param id the location for evidence upon building. (may not be null)
     * @param evidence the evidence to place at id. (may not be null or contain null values)
     */
    void setHole(String id, List<Evidence> evidence);

    /**
     * @return a source code string.
     */
    String buildDraft();
}
