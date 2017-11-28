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

import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.*;
import edu.rice.cs.caper.floodgage.application.floodgage.model.directive.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * A DirectiveXmlV1Parser that is based on the javax.xml package for xml parsing.
 */
public class JavaxXmlParser implements DirectiveXmlV1Parser
{
    @Override
    public Directive parse(String directiveString) throws ParseException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        /*
         * Parse directiveString into a Document and normalize.
         */
        Document doc;
        try
        {
            DocumentBuilder dBuilder;
            try
            {
                dBuilder = dbFactory.newDocumentBuilder();
            }
            catch (ParserConfigurationException e)
            {
                throw new RuntimeException(e);
            }

            doc = dBuilder.parse(new ByteArrayInputStream(directiveString.getBytes()));

            //http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
        }
        catch (SAXException | IOException e)
        {
            throw new ParseException(e);
        }


        /*
         * Take the (assumed root) <trials> element and collect the child <trial> elements.
         */
        List<Trial> trials;
        {
            final String TRIALS = "trials";
            NodeList list = doc.getElementsByTagName(TRIALS);

            if(list.getLength() < 1)
            {
                throw new ParseException("No " + TRIALS + " elements found");
            }
            else if(list.getLength() == 1)
            {
                Element trialsElement = (Element)list.item(0);
                trials = parseTrials(trialsElement);
            }
            else
            {
                throw new ParseException("Multiple " + TRIALS + " elements found");
            }
        }

        /*
         * Return a Directive containing the trials.
         */
        return Directive.make(trials);


    }

    /*
     * Collect the child <trial> elements.
     */
    private List<Trial> parseTrials(Element trialsElement) throws ParseException
    {
        List<Trial> trials = new LinkedList<>();

        NodeList list = trialsElement.getElementsByTagName("trial");
        for (int i = 0; i < list.getLength(); i++)
        {
            Trial trial = parseTrial((Element)list.item(i));
            trials.add(trial);
        }

        return trials;

    }

    /*
     * Create a Trial from a <trial> element.
     */
    private Trial parseTrial(Element trialElement) throws ParseException
    {
        if(trialElement == null)
            throw new NullPointerException("trialElement");

        // nulls ok because they signal element not present
        String description = getSingleChildElementTextContentOrNull(trialElement, "description");
        String draftProgramPath = getSingleChildElementTextContentOrNull(trialElement, "draftProgramPath");
        String expectedSketchProgramPath = getSingleChildElementTextContentOrNull(trialElement, "expectedSketchPath");

        List<Hole> holes = new LinkedList<>();
        {
            final String HOLES = "holes";
            NodeList list = trialElement.getElementsByTagName(HOLES);

            if(list.getLength() == 1)
            {
                Element holesElement = (Element)list.item(0);
                NodeList holesList = holesElement.getElementsByTagName("hole");
                for (int i = 0; i < holesList.getLength(); i++)
                {
                    Hole hole = parseHole((Element) holesList.item(i));
                    holes.add(hole);
                }
            }
            else if(list.getLength() > 1)
            {
                throw new ParseException("Multiple " + HOLES + " elements found");
            }
        }

        return Trial.make(description, draftProgramPath, expectedSketchProgramPath, holes);
    }

   /*
    * Create a Hole from a <hole> element.
    */
    private Hole parseHole(Element holeElement)
    {
        List<Evidence> evidences = new LinkedList<>();

        NodeList list = holeElement.getElementsByTagName("evidence");
        for (int i = 0; i < list.getLength(); i++)
        {
            Evidence evidence = parseEvidence((Element)list.item(i));
            evidences.add(evidence);
        }

        String id = getAttributeOrNull(holeElement, "id");
        return Hole.make(id, evidences);
    }

    /*
     * Create Evidence from evidenceElement
     */
    private Evidence parseEvidence(Element evidenceElement)
    {
        String type = getAttributeOrNull(evidenceElement, "type");
        return Evidence.make(type, evidenceElement.getTextContent());
    }

    private String getAttributeOrNull(Element item, String name)
    {
        if(item.hasAttribute(name))
            return item.getAttribute(name);

        return null;
    }

    private String getSingleChildElementTextContentOrNull(Element parentElement, String childElementName)
            throws ParseException
    {
        NodeList list = parentElement.getElementsByTagName(childElementName);
        if(list.getLength() < 1)
        {
            return null;
        }
        else if(list.getLength() == 1)
        {
            return list.item(0).getTextContent();
        }
        else
        {
            throw new ParseException("Multiple " + childElementName + " elements found");
        }
    }
}
