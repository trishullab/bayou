package edu.rice.cs.caper.floodgage.application.floodgage.model.evidence;

public class EvidenceType extends EvidenceBase
{
    public EvidenceType(String value)
    {
        super(value);
    }

    @Override
    public <R> R match(EvidenceCases<R> cases)
    {
        return cases.forType(this);
    }
}
