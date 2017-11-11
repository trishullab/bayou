package edu.rice.cs.caper.floodgage.application.floodgage.synthesizer;

import java.util.Arrays;
import java.util.List;

public class EchoSynthesizer implements Synthesizer
{
    @Override
    public List<String> synthesize(String draftProgram) throws SynthesizeException
    {
        return Arrays.asList(draftProgram, draftProgram);
    }
}
