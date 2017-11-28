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

import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Draft builder that is constructed with a "proto draft" string containing placeholders and substitutes those
 * placeholders with "triple slash" evidence strings (conforming to Bayou 1.1.0's expected "hole" format) upon
 * invocation of buildDraft().
 *
 * For example, a "proto draft" source string provided during construction might look like:
 *
 *     public class Foo
 *     {
 *         public void bar()
 *         {
 *             /// 1
 *
 *             /// 2
 *
 *             /// 3
 *         }
 *     }
 *
 * A caller could then, via setHole, associate the ids 1, 2, and three with evidence such as:
 *
 *    1 -> [new EvidenceCall("toString")]
 *    2 -> [new EvidenceCall("getClass")]
 *    3 -> [new EvidenceCall("toString")]
 *
 * Upon invocation of buildDraft, the builder would replace placeholders like "/// 1" with the corresponding Bayou
 * holes:
 *
 *
 *     public class Foo
 *     {
 *         public void bar()
 *         {
 *             /// call:toString
 *
 *             /// call:getClass
 *
 *             /// call:toString
 *         }
 *     }
 *
 * If a placeholder in the proto draft specifies an id unmapped by setHole, that placeholder substring will remain
 * unchanged in the string returned by buildDraft().
 */
public class DraftBuilderBayou1_1_0 implements DraftBuilder
{
    /*
     * Records the mappings of id to evidence specified by setHole(...)
     */
    private final Map<String, List<Evidence>> _idToEvidence = new HashMap<>();

    /*
     * The proto draft source.
     */
    private final String _protoDraft;

    /**
     * Constructs a new builder using the given proto draft source.
     *
     * @param protoDraft the source string used in building. (may not be null)
     */
    public DraftBuilderBayou1_1_0(String protoDraft)
    {
        if(protoDraft == null)
            throw new NullPointerException("protoDraft");

        _protoDraft = protoDraft;
    }

    @Override
    public void setHole(String id, List<Evidence> evidence)
    {
        if(id == null)
            throw new NullPointerException("id");

        if(evidence == null)
            throw new NullPointerException("evidence");

        for(Evidence e : evidence)
            if(e == null)
                throw new IllegalArgumentException("evidence may not contain null elements");

        _idToEvidence.put(id, evidence);
    }

    @Override
    public String buildDraft()
    {
        String draft = _protoDraft;

        for(String id : _idToEvidence.keySet())
        {
            /*
             * Build the Bayou 1.1.0 evidence hole using the evidence associated with id.
             */
            StringBuilder hole = new StringBuilder();
            {
                hole.append("\n{");

                for (Evidence evidence : _idToEvidence.get(id))
                    hole.append(createNotation(evidence)).append("\n");

                hole.append("}\n");
            }

            /*
             * Replace any occurence of the id placeholder with the hole.
             */
            draft = draft.replace("/// " + id, hole.toString());
        }

        return draft;
    }

    /*
     * Return the Bayou 1.1.0 evidence "tripple slash notation" form of evidence.
     */
    private String createNotation(Evidence evidence)
    {
        return evidence.match(new EvidenceCases<String>()
        {
            @Override
            public String forKeywords(EvidenceKeywords keywords)
            {
                return "/// " + keywords.getValue();
            }

            @Override
            public String forCase(EvidenceCall call)
            {
                return "/// call: " + call.getValue();
            }

            @Override
            public String forType(EvidenceType type)
            {
                return "/// type: " + type.getValue();
            }
        });
    }
}
