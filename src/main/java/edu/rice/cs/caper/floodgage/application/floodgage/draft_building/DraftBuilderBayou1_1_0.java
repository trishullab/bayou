package edu.rice.cs.caper.floodgage.application.floodgage.draft_building;

import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DraftBuilderBayou1_1_0 implements DraftBuilder
{
    private final Map<String, List<Evidence>> _idToEvidence = new HashMap<>();

    private final String _protoDraft;

    public DraftBuilderBayou1_1_0(String protoDraft)
    {
        _protoDraft = protoDraft;
    }

    @Override
    public void setHole(String id, List<Evidence> evidence)
    {
        _idToEvidence.put(id, evidence);
    }

    @Override
    public String buildDraft()
    {
        String draft = _protoDraft;

        for(String id : _idToEvidence.keySet())
        {
            StringBuilder hole = new StringBuilder();
            hole.append("\n{");
            for(Evidence evidence : _idToEvidence.get(id))
            {
                hole.append(createNotation(evidence)).append("\n");
            }
            hole.append("}\n");

            draft = draft.replace("/// " + id, hole.toString());
        }

        return draft;
    }

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
