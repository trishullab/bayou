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

import org.junit.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.*;

import java.io.*;
import java.util.*;

// THIS FILE IS A PART OF THE TESTPACK MAVEN INTEGRATION AND SHOULD NOT BE MODIFIED FOR TESTPACK CREATION.

/**
 * A series of context sensitive checks to be run on src/test/resources/trails.xml when "mvn package" is run by
 * the user to build a trial pack.
 */
public class ContextCheckerTest
{
    @Test
    public void checkValidTrailsXml()
    {
        /*
         * Get the byte contents of src/test/resources/trails.xml.
         */
        byte[] trailsXmlBytes;
        try
        {
            trailsXmlBytes = ResourceProvider.getResource("trials.xml");
        } catch (Exception e)
        {
            throw new RuntimeException("Could not access src/test/resources/trails.xml");
        }

        /*
         * Parse the trails xml bytes into a document.
         */
        Document doc;
        {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

            DocumentBuilder dBuilder;
            try
            {
                dBuilder = dbFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e)
            {
                throw new RuntimeException("Could not init parsing environment.");
            }

            try
            {
                doc = dBuilder.parse(new ByteArrayInputStream(trailsXmlBytes));
            } catch (Exception e)
            {
                throw new RuntimeException(e.getMessage());
            }

            //http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
            doc.getDocumentElement().normalize();
        }

        /*
         * Extract the trails element.
         */
        Element trialsElement;
        {

            final String TRIALS = "trials";
            NodeList list = doc.getElementsByTagName(TRIALS);

            if (list.getLength() < 1)
                throw new RuntimeException("No " + TRIALS + " element found.");

            if (list.getLength() > 1)
                throw new RuntimeException("Multiple " + TRIALS + " elements found.");

            trialsElement = (Element) list.item(0);
        }

        /*
         * Check trails only composed of trail elements.
         */
        assertChildrenOnlyNamed(trialsElement, "trial");

        /*
         * Extract the list of trail elements that are children of trials.
         */
        NodeList trialList;
        {
            final String TRIAL = "trial";
            trialList = trialsElement.getElementsByTagName(TRIAL);

            if (trialList.getLength() < 1)
                throw new RuntimeException("No " + TRIAL + " elements found.");
        }

        /*
         * Check each trial.
         */
        for (int i = 0; i < trialList.getLength(); i++)
        {

            Element trialElement = (Element) trialList.item(i);

            String DRAFT_PROGRAM_PATH = "draftProgramPath";
            String EXPECTED_SKETCH = "expectedSketchPath";

            /*
             * Check only expected children for trial.
             */
            assertChildrenOnlyNamed(trialElement, "description", DRAFT_PROGRAM_PATH, "holes", EXPECTED_SKETCH);

            /*
             * Check the existance of the draftProgramPath entry of the trail and fetch the contents of the file.
             */
            String draftProgramSource = getResourceFileSpecifiedByTrialChild(trialElement, i, DRAFT_PROGRAM_PATH);

            /*
             * If the trail has a expectedSketch element, ensure the identified resource file is present and can
             * be read.
             */
            try
            {
                // Don't care about the value of the output at this point, just want to ensure the file is present
                // and the contents can be read if the expectedSketch element is specified.
                // If there is a probelm reading the file (e.g. doesn't exist) a RuntimeException will be generated
                // by this call.
                getResourceFileSpecifiedByTrialChild(trialElement, i, EXPECTED_SKETCH);
            }
            catch (NoSuchElementException e)
            {
                // ok if element is not present, not a mandatory element
            }

            /*
             * Check the validity of the hole entries of each trail (excluding checking evidence parts which we do below).
             */
            checkTrailExcludingEvidence(trialElement, i, draftProgramSource);

           /*
            * Check all evidence elements of trail.
            */
            checkTrailEvidence(trialElement, i);
        }

    }

    /*
     * Check all evidence elements of trail.
     */
    private void checkTrailEvidence(Element trialElement, int trailElementIndex)
    {
        NodeList holeList = getHoleListFromTrailElement(trialElement);

        for (int j = 0; j < holeList.getLength(); j++)
        {
            Element hole = (Element) holeList.item(j);
            NodeList evidenceList = hole.getElementsByTagName("evidence");

            for (int k = 0; k < evidenceList.getLength(); k++)
            {
                Element evidence = (Element) evidenceList.item(k);
                assertChildrenOnlyNamed(evidence, ""); // assert evidence has no element children

                /*
                 * Ensure the evidence has a type.
                 */
                final String TYPE = "type";
                String type = evidence.getAttribute(TYPE);

                if (type == null)
                {
                    throw new RuntimeException("Missing " + TYPE + " attribute for evidence " + (k + 1) + " of hole " +
                            (j + 1) + " of trail " + (trailElementIndex + 1) + ".");
                }

                /*
                 * Ensure the evidence type is recognized.
                 */
                if (!type.equals("keywords") && !type.equals("call") && !type.equals("type") && !type.equals("context"))
                {
                    throw new RuntimeException("Unknown type " + type + "for evidence " + (k + 1) + " of hole " +
                            (j + 1) + " of trail " + (trailElementIndex + 1) + ".");
                }
            }
        }
    }

    /*
     * Check the validity of the hole entries of each trail (excluding checking evidence parts).
     */
    private void checkTrailExcludingEvidence(Element trialElement, int trailElementIndex, String draftProgramSource)
    {
        NodeList holeList = getHoleListFromTrailElement(trialElement);

        Set<String> seenHoleIds = new HashSet<String>(); // track seen to ensure no duplicates
        for (int j = 0; j < holeList.getLength(); j++)
        {
            Element hole = (Element) holeList.item(j);
            assertChildrenOnlyNamed(hole, "evidence");

            /*
             * Check hole id is present and unique.
             */
            String id;
            {
                id = hole.getAttribute("id");

                if (id == null)
                {
                    throw new RuntimeException("Missing id attirbute for hole " + (j + 1) + " of trail " +
                            (trailElementIndex + 1) + ".");
                }

                if (seenHoleIds.contains(id))
                {
                    throw new RuntimeException("Duplicate id attirbute for hole " + (j + 1) + " of trail " +
                            (trailElementIndex + 1) + ".");
                }
            }

            seenHoleIds.add(id);

            /*
             * Check that the draft program has a hole with the specified id.
             */
            if (!draftProgramSource.contains("/// " + id))
            {
                throw new RuntimeException("Draft program does not contain id " + id + " for hole " + (j + 1) +
                        " of trail " + (trailElementIndex + 1) + ".");
            }

            /*
             * Check the hole has at least one evidence (we wont check for well formed here, but below).
             */
            NodeList evidenceList = hole.getElementsByTagName("evidence");

            if (evidenceList.getLength() < 1)
                throw new RuntimeException("Missing evidence for hole " + (j + 1) + " of trail " +
                        (trailElementIndex + 1) + ".");
        }
    }

    /*
     * Check the existance of the child element named childName of trailElement and return the string contents of
     * file path specified by the child element's value (relative to the resources directory).
     */
    private String getResourceFileSpecifiedByTrialChild(Element trialElement, int trailElementIndex, String childName)
    {
        /*
         * Get the path entry of trailElement identified by child element childName.
         */
        String path;
        {
            path = getSingleChildElementTextContent(trialElement, childName);

            if (path == null)
            {
                throw new NoSuchElementException("Missing " + childName + " for trail " + (trailElementIndex + 1));
            }
        }

        /*
         * Read the string contents of the path relative to the resources folder.
         */
        String fileContents;
        try
        {
            byte[] fileContentsBytes = ResourceProvider.getResource(path);
            fileContents = new String(fileContentsBytes);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not read resource " + path);
        }

        return fileContents;

    }

    /*
     * Return the (possibly empty) list of holes for the trial.
     */
    private NodeList getHoleListFromTrailElement(Element trialElement)
    {
        final String HOLES = "holes";
        NodeList holesList = trialElement.getElementsByTagName(HOLES);

        if (holesList.getLength() > 1)
            throw new RuntimeException("Multiple " + HOLES + " elements found");

        Element holesElement = (Element) holesList.item(0);
        assertChildrenOnlyNamed(holesElement, "hole");

        return holesElement.getElementsByTagName("hole");
    }

    /*
     * Assert that every child element of the given element has one of the given names.
     */
    private void assertChildrenOnlyNamed(Element element, String... names)
    {
        HashSet<String> allowedNames = new HashSet<String>(Arrays.asList(names));

        NodeList children = element.getChildNodes();

        for (int i = 0; i < children.getLength(); i++)
        {
            Node child = children.item(i);
            if (!(child instanceof Element))
                continue;

            if (!allowedNames.contains(child.getNodeName()))
                throw new RuntimeException("Unexpected element " + child.getNodeName());

        }
    }

    /*
     * Get the text contents of the unique child element of the given parent element.
     */
    private String getSingleChildElementTextContent(Element parentElement, String childElementName)
    {
        NodeList list = parentElement.getElementsByTagName(childElementName);
        if (list.getLength() < 1)
        {
            return null;
        } else if (list.getLength() == 1)
        {
            return list.item(0).getTextContent();
        } else
        {
            throw new IllegalArgumentException("Multiple " + childElementName + " elements found");
        }
    }
}
