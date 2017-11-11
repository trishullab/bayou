package edu.rice.cs.caper.floodgage.application.floodgage.synthesizer;

import edu.rice.cs.caper.bayou.core.bayou_services_client.api_synthesis.ApiSynthesisClient;
import edu.rice.cs.caper.bayou.core.bayou_services_client.api_synthesis.SynthesisError;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SynthesizerBayou_1_1_0 implements Synthesizer
{
    private final String _host;

    private final int _port;

    public SynthesizerBayou_1_1_0(String host, int port)
    {
        _host = host;
        _port = port;
    }

    @Override
    public List<String> synthesize(String draftProgram) throws SynthesizeException
    {
        try
        {
            return new ApiSynthesisClient(_host, _port).synthesise(draftProgram, 10);
        }
        catch (IOException | SynthesisError e)
        {
            throw new SynthesizeException(e);
        }
    }
}
