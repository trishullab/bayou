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
package edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser;

import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Directive;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Wrapper for parse(...)
 */
public interface DirectiveStringParser
{
    /**
     * Parses the given string (in unspecified format) into a Directive instance.
     *
     * Derived types should place more stringent specifications on the format of directiveString.
     *
     * @param directiveString the Directive in string format.
     * @return a Directive instance corresponding to directiveString
     * @throws ParseException if directiveString is not corresponding to a Directive instance.
     */
    Directive parse(String directiveString) throws ParseException;
}
