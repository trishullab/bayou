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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class JavaxXmlParser implements DirectiveXmlV1Parser
{
    @Override
    public Directive parse(String directiveString) throws ParseException
    {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();


        DocumentBuilder dBuilder;
        try
        {
            dBuilder = dbFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            throw new RuntimeException(e);
        }


        Document doc = null;
        try
        {
            doc = dBuilder.parse(new ByteArrayInputStream(directiveString.getBytes()));
        }
        catch (SAXException | IOException e)
        {
            throw new ParseException(e);
        }

        //http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
        doc.getDocumentElement().normalize();



        List<Trial> trails;
        {
            final String TRIALS = "trials";
            NodeList list = doc.getElementsByTagName(TRIALS);

            if(list.getLength() < 1)
            {
                return null;
            }
            else if(list.getLength() == 1)
            {
                Element trialsElement = (Element)list.item(0);
                trails = parseTrials(trialsElement);
            }
            else
            {
                throw new ParseException("Multiple " + TRIALS + " elements found");
            }
        }

        return Directive.make(trails);


    }

//    private TestSuite parseTestSuite(Element testSuiteNode) throws ParseException
//    {
//        String passProgramPath = getSingleChildElementTextContent(testSuiteNode, "passProgramPath");
//        String testSuitePath = getSingleChildElementTextContent(testSuiteNode, "testSuitePath");
//        String resourcePath = getSingleChildElementTextContent(testSuiteNode, "resourcePath");
//
//        List<Trial> trails;
//        {
//            final String TRIALS = "trials";
//            NodeList list = testSuiteNode.getElementsByTagName(TRIALS);
//
//            if(list.getLength() < 1)
//            {
//                return null;
//            }
//            else if(list.getLength() == 1)
//            {
//                Element trialsElement = (Element)list.item(0);
//                trails = parseTrials(trialsElement);
//            }
//            else
//            {
//                throw new ParseException("Multiple " + TRIALS + " elements found");
//            }
//        }
//
//        return TestSuite.make(testSuitePath, passProgramPath, resourcePath, trails);
//    }

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

    private Trial parseTrial(Element trialElement) throws ParseException
    {
        String description = getSingleChildElementTextContent(trialElement, "description");
        String draftProgramPath = getSingleChildElementTextContent(trialElement, "draftProgramPath");
        String expectedSketchProgramPath = getSingleChildElementTextContent(trialElement, "expectedSketchPath");

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

    private Hole parseHole(Element holeElement)
    {
        List<Evidence> evidences = new LinkedList<>();

        NodeList list = holeElement.getElementsByTagName("evidence");
        for (int i = 0; i < list.getLength(); i++)
        {
            Evidence evidence = parseEvidence((Element)list.item(i));
            evidences.add(evidence);
        }

        String id = getAttribute(holeElement, "id");
        return Hole.make(id, evidences);
    }

    private Evidence parseEvidence(Element item)
    {
        String type = getAttribute(item, "type");
        return Evidence.make(type, item.getTextContent());
    }

    private String getAttribute(Element item, String name)
    {
        if(item.hasAttribute(name))
            return item.getAttribute(name);

        return null;
    }

    private String getSingleChildElementTextContent(Element parentElement, String childElementName) throws ParseException
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
