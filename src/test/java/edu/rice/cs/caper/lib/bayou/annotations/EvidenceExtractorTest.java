//package edu.rice.cs.caper.lib.bayou.annotations;
//
//
//import org.junit.Assert;
//import org.junit.Test;
//
//import java.io.IOException;
//
///**
// * Created by barnett on 5/10/17.
// */
//public class EvidenceExtractorTest
//{
//    @Test
//    public void testExecute() throws IOException
//    {
//        String searchCode = " \n" +
//                "import edu.rice.bayou.annotations.Evidence;\n" +
//                "\n" +
//                "public class TestIODog {\n" +
//                "\n" +
//                "    @Evidence(keywords = \"read buffered line from the file\")\n" +
//                "    void __bayou_fill(String file) {\n" +
//                "    }\n" +
//                "\n" +
//                "}\n";
//        String evidence = new EvidenceExtractor().execute(searchCode);
//
//        Assert.assertEquals("null", evidence);
//    }
//}
