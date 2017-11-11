package edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser;

import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Directive;
import org.xml.sax.SAXException;

import java.io.IOException;

public interface DirectiveStringParser
{
    Directive parse(String directiveString) throws ParseException;
}
