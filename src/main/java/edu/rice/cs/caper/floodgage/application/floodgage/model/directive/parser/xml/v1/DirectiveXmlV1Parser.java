package edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.xml.v1;

import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.DirectiveStringParser;

public interface DirectiveXmlV1Parser extends DirectiveStringParser
{
    static DirectiveXmlV1Parser makeDefault()
    {
        return new JavaxXmlParser();
    }
}
