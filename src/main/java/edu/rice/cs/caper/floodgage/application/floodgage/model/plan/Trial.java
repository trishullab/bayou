package edu.rice.cs.caper.floodgage.application.floodgage.model.plan;

public interface Trial
{
    String getDescription();

    String getDraftProgramSource();

    String getDraftProgramClassName();

    boolean containsSketchProgramSource();

    String tryGetSketchProgramSource(String failValue);
}
