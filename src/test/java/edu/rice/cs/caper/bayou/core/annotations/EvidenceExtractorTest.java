package edu.rice.cs.caper.bayou.core.annotations;


import edu.rice.cs.caper.bayou.core.annotations.EvidenceExtractor;
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
        String searchCode = "import edu.rice.bayou.annotations.Evidence;\n" +
                "import android.content.Context;\n" +
                "\n" +
                "public class TestDialog {\n" +
                "\n" +
                "    @Evidence(apicalls = {\"setTitle\", \"setMessage\"})\n" +
                "    @Evidence(types = {\"AlertDialog\"})\n" +
                "    void __bayou_fill(Context c, String str1, String str2) {\n" +
                "\n" +
                "    }   \n" +
                "\n" +
                "}\n";
        String evidence = new EvidenceExtractor().execute(searchCode, "");

        Assert.assertNotNull(evidence);
    }
}
