package edu.rice.cs.caper.floodgage.application.floodgage.model.evidence;

public interface Evidence
{
    String getValue();

    <R> R match(EvidenceCases<R> cases);
}
