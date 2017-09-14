/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;

import edu.rice.cs.caper.bayou.core.lexer.UnexpectedEndOfCharacters;
import edu.rice.cs.caper.bayou.core.lexer.ccll._1_0.*;
import edu.rice.cs.caper.bayou.core.parser.evidencel._1_0.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

/**
 * And ApiSynthesizer decorator (pattern) that replaces evidence of the style /// with
 * Java method invocations from class in edu.rice.cs.caper.bayou.annotations.Evidence.
 */
public class ApiSynthesizerRewriteEvidenceDecorator implements ApiSynthesizer
{
    // thrown when we see evidence of the form "/// [some id]:" where we don't recognize [some id] from our known
    // vocabulary.
    private static class UnknownType extends Throwable
    {
        final String Type;

        UnknownType(String typeIdent)
        {
            Type = typeIdent;
        }
    }

    /**
     * Place to send application logging information.
     */
    private static final Logger _logger =
            LogManager.getLogger(ApiSynthesizerRewriteEvidenceDecorator.class.getName());

    /**
     * The underlying synthesizer this class decorates.
     */
    private final ApiSynthesizer _synthesizer;

    /**
     * @param synthesisStrategy the underlying synthesizer to receive the evidence rewritten code.
     */
    public ApiSynthesizerRewriteEvidenceDecorator(ApiSynthesizer synthesisStrategy)
    {
        if(synthesisStrategy == null)
            throw new NullPointerException("synthesisStrategy");

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
            throw new SynthesiseException(e);
        }
        _logger.trace("rewrittenCode:" + rewrittenCode);

        if(sampleCount != null)
            return _synthesizer.synthesise(rewrittenCode, maxProgramCount, sampleCount);

        return _synthesizer.synthesise(rewrittenCode, maxProgramCount);
    }

    // replace instances of /// in code with calls to methods in edu.rice.cs.caper.bayou.annotations.Evidence.
    // n.b. static for testing without construction
    static String rewriteEvidence(String code) throws ParseException, UnexpectedEndOfCharacters
    {
        StringBuilder rewrittenCodeAccum = new StringBuilder();

        /*
         * Break code down into a sequence of substrings such as /// lines
         */
        Iterable<Token> codeTokens = CcllLexer.makeDefault().lex(code);

        /*
         * Process each token of codeTokens rewriting the token's content if it starts with /// and otherwise
         * preserving the token as is.
         */
        for(Token token :  codeTokens)
        {
            String transformedLexeme = token.getType().match(new TokenTypeCases<String, ParseException>()
            {
                @Override
                public String forLineComment(TokenTypeLineComment lineComment) throws ParseException
                {
                    if(!token.getLexeme().startsWith("///"))
                        return token.getLexeme(); // not ///, append unchanged

                    return makeEvidenceFromComment(token.getLexeme()); // is ///, possibly rewrite
                }

                @Override
                public String forOther(TokenTypeOther other)
                {
                    return token.getLexeme(); // not ///, append unchanged
                }

                @Override
                public String forString(TokenTypeString string)
                {
                    return token.getLexeme(); // not ///, append unchanged
                }

                @Override
                public String forBlockComment(TokenTypeBlockComment blockComment)
                {
                    return token.getLexeme(); // not ///, append unchanged
                }
            });

            rewrittenCodeAccum.append(transformedLexeme);
        }

        return rewrittenCodeAccum.toString();
    }

    // attempt to determine the corresponding edu.rice.cs.caper.bayou.annotations.Evidence call from the
    // /// short hand notation.
    static String makeEvidenceFromComment(String tripleSlashComment) throws ParseException
    {
        if(!tripleSlashComment.startsWith("///"))
            throw new IllegalArgumentException("tripleSlashComment must start with ///");

        String evidence = tripleSlashComment.substring(3);
        SourceUnitNode root = EvidenceLParser.makeDefault().parse(evidence);

        if(root.getElements().isEmpty()) // nothing followed the /// so just return it as is
            return tripleSlashComment;

        StringBuilder rewriteAccum = new StringBuilder();
        for(EvidenceElement element : root.getElements())
        {
            // element something like:
            // foo
            // or
            // foo, bar
            // or
            // call: foo
            // or
            // calls: foo, bar

            List<String> idents =  element.getIdentifierList().getIdentifiers().stream()
                                                            .map(IdentifierNode::getIdentifier)
                                                            .collect(Collectors.toList());
            // in the examples above idents would be:
            // ["foo"]
            // ["foo", "bar"]
            // ["foo"]
            // ["foo", "bar"]

            String evidencePrefix;
            // edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(
            // or
            // edu.rice.cs.caper.bayou.annotations.Evidence.types(
            // or
            // edu.rice.cs.caper.bayou.annotations.Evidence.context(
            try
            {
                evidencePrefix = determineEvidenceType(element);
            }
            catch (UnknownType unknownType)
            {
                throw new ParseException("unknown type "  + unknownType.Type); // had [ident]: but we don't recognize [ident]
            }

            rewriteAccum.append(evidencePrefix);
            rewriteAccum.append(String.join("\", \"",idents));
            rewriteAccum.append("\");");

        }

        rewriteAccum.append("\n");
        return rewriteAccum.toString();

    }

    // if no type identifier (e.g. "call:") then assume the evidence type is apicalls.
    private static String determineEvidenceType(EvidenceElement element) throws UnknownType
    {
        return element.match(new EvidenceElementCases<String, UnknownType>()
        {
            @Override
            public String forWithoutTypeIdent(EvidenceElementWithoutTypeIdentifierNode evidenceElement)
            {
                return "edu.rice.cs.caper.bayou.annotations.Evidence.freeform(\"";
            }

            @Override
            public String forWithTypeIdent(EvidenceElementWithTypeIdentifierNode evidenceElement) throws UnknownType
            {
                String typeIdent = evidenceElement.getTypeIdentifier().getIdentifier();
                switch (typeIdent)
                {
                    case "call":
                    case "calls":
                        return "edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"";
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
