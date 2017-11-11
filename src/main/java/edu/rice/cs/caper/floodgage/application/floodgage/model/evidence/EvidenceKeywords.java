package edu.rice.cs.caper.floodgage.application.floodgage.model.evidence;

public class EvidenceKeywords extends EvidenceBase
{
    public EvidenceKeywords(String value)
    {
        super(value);
    }

    @Override
    public <R> R match(EvidenceCases<R> cases)
    {
        return cases.forKeywords(this);
    }
}
