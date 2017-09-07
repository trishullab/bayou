package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;


import edu.rice.cs.caper.bayou.core.lexer.UnexpectedEndOfCharacters;
import edu.rice.cs.caper.bayou.core.lexer.ccll._1_0.*;
import edu.rice.cs.caper.bayou.core.parser.evidencel._1_0.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

public class ApiSynthesizerRewriteEvidenceDecorator implements ApiSynthesizer
{
    private static class UnknownType extends Throwable
    {
        public UnknownType(String typeIdent)
        {
        }
    }

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
        catch (UnexpectedEndOfCharacters | ParseException e)
        {
            _logger.error(e.getMessage(), e);
            throw new SynthesiseException(e);
        }
        _logger.trace("rewrittenCode:" + rewrittenCode);

        if(sampleCount != null)
            return _synthesizer.synthesise(rewrittenCode, maxProgramCount, sampleCount);

        return _synthesizer.synthesise(rewrittenCode, maxProgramCount);
    }

    // n.b. static for testing without construction
    static String rewriteEvidence(String code) throws ParseException, UnexpectedEndOfCharacters
    {
        StringBuilder newCode = new StringBuilder();

        for(Token token :  CcllLexer.makeDefault().lex(code))
        {
            String transformedLexeme = token.getType().match(new TokenTypeCases<String, ParseException>()
            {
                @Override
                public String forLineComment(TokenTypeLineComment lineComment) throws ParseException
                {
                    if(!token.getLexeme().startsWith("///"))
                        return token.getLexeme();

                    return makeEvidenceFromComment(token.getLexeme());
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

    private static String makeEvidenceFromComment(String tripleSlashComment) throws ParseException
    {
        if(!tripleSlashComment.startsWith("///"))
            throw new IllegalArgumentException("tripleSlashComment must start with ///");

        String evidence = tripleSlashComment.substring(3);
        SourceUnitNode root = EvidenceLParser.makeDefault().parse(evidence);

        if(root.getElements().isEmpty())
            return tripleSlashComment;

        StringBuilder rewriteAccum = new StringBuilder();
        for(EvidenceElement element : root.getElements())
        {
            List<String> args =  element.getIdentifierList().getIdentifiers().stream()
                                                            .map(IdentifierNode::getIdentifier)
                                                            .collect(Collectors.toList());

            String evidencePrefix;
            try
            {
                evidencePrefix = determineEvidenceType(element);
            }
            catch (UnknownType unknownType)
            {
                return tripleSlashComment;
            }

            rewriteAccum.append(evidencePrefix);
            rewriteAccum.append(String.join("\", \"",args));
            rewriteAccum.append("\");");

        }

        rewriteAccum.append("\n");
        return rewriteAccum.toString();

    }

    private static String determineEvidenceType(EvidenceElement element) throws UnknownType
    {
        return element.match(new EvidenceElementCases<String, UnknownType>()
        {
            final String _apiCallsPrefix = "edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"";

            @Override
            public String forWithoutTypeIdent(EvidenceElementWithoutTypeIdentifierNode evidenceElement)
            {
                return _apiCallsPrefix;
            }

            @Override
            public String forWithTypeIdent(EvidenceElementWithTypeIdentifierNode evidenceElement) throws UnknownType
            {
                String typeIdent = evidenceElement.getTypeIdentifier().getIdentifier();
                switch (typeIdent)
                {
                    case "call":
                    case "calls":
                        return _apiCallsPrefix;
                    case "type":
                    case "types":
                        return "edu.rice.cs.caper.bayou.annotations.Evidence.types(\"";
                    case "context":
                        return "edu.rice.cs.caper.bayou.annotations.Evidence.context(\"";
                    default:
                        throw new UnknownType(typeIdent);
                }
            }
        });
    }


}
