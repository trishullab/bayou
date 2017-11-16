package edu.rice.cs.caper.floodgage.application.floodgage.synthesizer;

import java.util.List;

public interface Synthesizer
{
    List<String> synthesize(String draftProgram) throws SynthesizeException;
}
