package edu.rice.cs.caper.floodgage.application.floodgage.model.evidence;

public interface EvidenceCases<R>
{
    R forKeywords(EvidenceKeywords keywords);

    R forCase(EvidenceCall call);

    R forType(EvidenceType type);
}
