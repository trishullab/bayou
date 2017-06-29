package edu.rice.cs.caper.bayou.annotations;


import edu.rice.cs.caper.bayou.core.synthesizer.EvidenceExtractor;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by barnett on 5/10/17.
 */
public class EvidenceExtractorTest
{
    @Test
    public void testExecute() throws IOException
    {
        String searchCode = "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class TestDialog {\n" +
                "\n" +
                "    void test(Context c, String str1, String str2) {\n" +
                "       {\n" +
                "       Evidence.apicalls(\"setTitle\", \"setMessage\");\n" +
                "       Evidence.types(\"AlertDialog\");\n" +
                "       }\n" +
                "\n" +
                "    }   \n" +
                "\n" +
                "}\n";
        String evidence = new EvidenceExtractor().execute(searchCode, "");

        Assert.assertNotNull(evidence);
    }
}
