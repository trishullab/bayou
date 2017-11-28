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
package edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.xml.v1;

import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.Directive;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.DirectiveStringParser;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.ParseException;

/**
 * Parses "trials.xml" formated files of the general form:
 *
 * <trials>
 *   <trial>
 *     <description>...</description>
 *     <draftProgramPath>...</draftProgramPath>
 *     <expectedSketchPath>...</expectedSketchPath>
 *     <holes>
 *       <hole id="...">
 *         <evidence type="...">...</evidence>
 *         <evidence type="...">...</evidence>
 *       </hole>
 *     </holes>
 *   </trial>
 * </trials>
 */
public interface DirectiveXmlV1Parser extends DirectiveStringParser
{
    /**
     * Parses the given xml string into a Directive instance.
     *
     * @param directiveString the Directive in xml format.
     * @return a Directive instance corresponding to directiveString
     * @throws ParseException if directiveString is not a valid trials.xml file
     */
    Directive parse(String directiveString) throws ParseException;

    /**
     * @return a new DirectiveXmlV1Parser instance of unspecified implementation.
     */
    static DirectiveXmlV1Parser makeDefault()
    {
        return new JavaxXmlParser();
    }
}
