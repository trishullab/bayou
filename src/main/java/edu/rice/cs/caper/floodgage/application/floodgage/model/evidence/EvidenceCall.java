package edu.rice.cs.caper.floodgage.application.floodgage.model.evidence;

public class EvidenceCall extends EvidenceBase
{
    public EvidenceCall(String value)
    {
        super(value);
    }

    @Override
    public <R> R match(EvidenceCases<R> cases)
    {
        return cases.forCase(this);
    }
}
