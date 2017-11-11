package edu.rice.cs.caper.floodgage.application.floodgage.model.evidence;

abstract class EvidenceBase implements Evidence
{
    private final String _value;

    protected EvidenceBase(String value)
    {
        _value = value;
    }

    @Override
    public String getValue()
    {
        return _value;
    }
}
