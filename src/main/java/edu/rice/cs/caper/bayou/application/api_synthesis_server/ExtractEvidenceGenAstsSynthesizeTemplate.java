package edu.rice.cs.caper.bayou.application.api_synthesis_server;

import edu.rice.cs.caper.bayou.core.synthesizer.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.util.List;

/**
 * An ApiSynthesisStrategy strategy that peforms three steps:
 *
 * 1.) Take the given code and extract evidence via the constructor provided EvidenceExtractor.
 * 2.) Take the evidence yielded by step 1.) and generate asts via generateAsts(...)
 * 3.) Take the asts yielded by step 2.) and perform synthesis va the constructor provided Synthesizer.
 */
abstract class ExtractEvidenceGenAstsSynthesizeTemplate implements ApiSynthesisStrategy
{
    /**
     * Place to send logging information.
     */
    private static final Logger _logger =
            LogManager.getLogger(ExtractEvidenceGenAstsSynthesizeTemplate.class.getName());

    private final EvidenceExtractor _evidenceExtractor;

    private final Synthesizer _synthesizer;

    ExtractEvidenceGenAstsSynthesizeTemplate(EvidenceExtractor evidenceExtractor, Synthesizer synthesizer)
    {
        _evidenceExtractor = evidenceExtractor;
        _synthesizer = synthesizer;
    }

    @Override
    public Iterable<String> synthesise(String searchCode, int maxProgramCount) throws SynthesiseException, ParseException
    {
        return synthesiseHelp(searchCode, maxProgramCount, null);
    }

    @Override
    public Iterable<String> synthesise(String searchCode, int maxProgramCount, int sampleCount) throws SynthesiseException, ParseException
    {
        return synthesiseHelp(searchCode, maxProgramCount, sampleCount);
    }

    private Iterable<String> synthesiseHelp(String code, int maxProgramCount, Integer sampleCount) throws SynthesiseException, ParseException
    {
        _logger.debug("entering");

        /*
         * Extract a description of the evidence in the search code that should guide AST results generation.
         */
        String evidence = _evidenceExtractor.extract(code);

        /*
         * Provide evidence to generate solution ASTs.
         */
        String astsJson = generateAsts(evidence, maxProgramCount, sampleCount);

        /*
         * Synthesise results from the code and asts and return.
         */
        List<String> synthesizedPrograms;
        try
        {
            synthesizedPrograms = _synthesizer.synthesize(code, astsJson);
            _logger.trace("synthesizedPrograms: " + synthesizedPrograms);

            // unsure if execute always returns n output for n ast input.
            if(synthesizedPrograms.size() > maxProgramCount)
                synthesizedPrograms = synthesizedPrograms.subList(0, maxProgramCount);
        }
        catch (IOException|ParseException e)
        {
            _logger.debug("exiting");
            throw new SynthesiseException(e);
        }

        _logger.debug("exiting");
        return synthesizedPrograms;
    }

    abstract String generateAsts(String evidence, int maxProgramCount, Integer sampleCount) throws SynthesiseException;
}
