package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;


import edu.rice.cs.caper.bayou.core.lexer.ccll._1_0.*;
import edu.rice.cs.caper.bayou.core.synthesizer.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ApiSynthesizerRewriteEvidenceDecorator implements ApiSynthesizer
{
    /**
     * Place to send application logging information.
     */
    private static final Logger _logger =
            LogManager.getLogger(ApiSynthesizerRewriteEvidenceDecorator.class.getName());

    private final ApiSynthesizer _synthesizer;

    public ApiSynthesizerRewriteEvidenceDecorator(ApiSynthesizer synthesisStrategy)
    {
        _synthesizer = synthesisStrategy;
    }

    @Override
    public Iterable<String> synthesise(String code, int maxProgramCount) throws SynthesiseException
    {
        return synthesiseHelp(code, maxProgramCount, null);
    }

    @Override
    public Iterable<String> synthesise(String code, int maxProgramCount, int sampleCount)
            throws SynthesiseException
    {
        return synthesiseHelp(code, maxProgramCount, sampleCount);
    }

    private Iterable<String> synthesiseHelp(String code, int maxProgramCount, Integer sampleCount)
            throws SynthesiseException
    {
        String rewrittenCode;
        try
        {
            rewrittenCode = rewriteEvidence(code);
        }
        catch (UnexpectedEndOfCharacters unexpectedEndOfCharacters)
        {
            _logger.error(unexpectedEndOfCharacters.getMessage(), unexpectedEndOfCharacters);
            throw new SynthesiseException(unexpectedEndOfCharacters);
        }
        _logger.trace("rewrittenCode:" + rewrittenCode);

        if(sampleCount != null)
            return _synthesizer.synthesise(rewrittenCode, maxProgramCount, sampleCount);

        return _synthesizer.synthesise(rewrittenCode, maxProgramCount);
    }

    // n.b. static for testing without construction
    static String rewriteEvidence(String code) throws UnexpectedEndOfCharacters
    {
        StringBuilder newCode = new StringBuilder();

        for(Token token :  new CcllLexerDefault().lex(code))
        {
            String transformedLexeme = token.getType().match(new TokenTypeCases<String, RuntimeException>()
            {
                @Override
                public String forLineComment(TokenTypeLineComment lineComment)
                {
                    String lexeme = token.getLexeme();

                    if(!lexeme.startsWith("///") || !lexeme.contains(":"))
                        return token.getLexeme();

                    String uncommentedLexeme = lexeme.replace("///", "");

                    String[] parts = uncommentedLexeme.split(":");

                    if(parts.length !=2)
                        return token.getLexeme();

                    switch (parts[0].trim())
                    {
                        case "call":
                            return "edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"" +  parts[1].trim() + "\");\n";
                        case "type":
                            return "edu.rice.cs.caper.bayou.annotations.Evidence.types(\"" +  parts[1].trim() + "\");\n";
                        case "context":
                            return "edu.rice.cs.caper.bayou.annotations.Evidence.context(\"" +  parts[1].trim() + "\");\n";
                        default:
                    }       return token.getLexeme();

                }

                @Override
                public String forOther(TokenTypeOther other)
                {
                    return token.getLexeme();
                }

                @Override
                public String forString(TokenTypeString string)
                {
                    return token.getLexeme();
                }

                @Override
                public String forBlockComment(TokenTypeBlockComment blockComment)
                {
                    return token.getLexeme();
                }
            });

            newCode.append(transformedLexeme);
        }

        return newCode.toString();
    }
}
