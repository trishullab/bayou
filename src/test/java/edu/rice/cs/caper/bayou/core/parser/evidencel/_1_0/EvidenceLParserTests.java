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
package edu.rice.cs.caper.bayou.core.parser.evidencel._1_0;

import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.Token;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.TokenTypeColon;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.TokenTypeComma;
import edu.rice.cs.caper.bayou.core.lexer.evidencel._1_0.TokenTypeIdentifier;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class EvidenceLParserTests
{
    protected abstract EvidenceLParser makeParser();

    @Test
    public void parseEmpty() throws ParseException
    {
        EvidenceLParser parser = makeParser();

        SourceUnitNode unit = parser.parse(Collections.emptyList());

        Assert.assertEquals(0, unit.getElements().size());

    }

    @Test
    public void parseIdentifier() throws ParseException
    {
        EvidenceLParser parser = makeParser();

        SourceUnitNode unit = parser.parse(Collections.singletonList(Token.make("ident", new TokenTypeIdentifier())));
        List<EvidenceElement> evidence = unit.getElements();

        Assert.assertEquals(1, evidence.size());

    }



    @Test
    public void parseMultiCall() throws ParseException
    {
        EvidenceLParser parser = makeParser();

        // calls: setTitle, setMessage
        SourceUnitNode unit = parser.parse(Arrays.asList(
                Token.make("calls", new TokenTypeIdentifier()),
                Token.make(":", new TokenTypeColon()),
                Token.make("setTitle", new TokenTypeIdentifier()),
                Token.make(",", new TokenTypeComma()),
                Token.make("setMessage", new TokenTypeIdentifier())));

        List<EvidenceElement> evidences = unit.getElements();

        Assert.assertEquals(1, evidences.size());

        EvidenceElementWithTypeIdentifierNode evidence = (EvidenceElementWithTypeIdentifierNode)evidences.get(0);

        Assert.assertEquals("calls", evidence.getTypeIdentifier().getIdentifier());

        Assert.assertEquals(2, evidence.getIdentifierList().getIdentifiers().size());
        Assert.assertEquals("setTitle", evidence.getIdentifierList().getIdentifiers().get(0).getIdentifier());
        Assert.assertEquals("setMessage", evidence.getIdentifierList().getIdentifiers().get(1).getIdentifier());

    }


}
