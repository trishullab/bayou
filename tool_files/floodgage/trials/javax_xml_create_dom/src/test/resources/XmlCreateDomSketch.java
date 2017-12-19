import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;


public class XmlCreateDomSketch
{
    public void accept(String elemName)
    {
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            // root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement(elemName);
            doc.appendChild(rootElement);

        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }
}
