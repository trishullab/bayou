package edu.rice.cs.caper.floodgage.application.floodgage.draft_building;

import edu.rice.cs.caper.floodgage.application.floodgage.model.evidence.Evidence;

import java.util.List;

public interface DraftBuilder
{
    void setHole(String id, List<Evidence> evidence);

    String buildDraft();
}
