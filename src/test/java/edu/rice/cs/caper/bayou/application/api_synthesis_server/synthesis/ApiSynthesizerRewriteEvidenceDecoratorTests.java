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
package edu.rice.cs.caper.bayou.application.api_synthesis_server.synthesis;

import edu.rice.cs.caper.bayou.core.lexer.UnexpectedEndOfCharacters;
import edu.rice.cs.caper.bayou.core.parser.evidencel._1_0.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ApiSynthesizerRewriteEvidenceDecoratorTests
{
    class ApiSynthesizerJustReturn implements ApiSynthesizer
    {

        @Override
        public Iterable<String> synthesise(String code, int maxProgramCount) throws SynthesiseException
        {
            return Collections.singletonList(code);
        }

        @Override
        public Iterable<String> synthesise(String code, int maxProgramCount, int sampleCount) throws SynthesiseException
        {
            return Collections.singletonList(code);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testConstruction()
    {
        new ApiSynthesizerRewriteEvidenceDecorator(null);
    }

    @Test
    public void testSynthesize1() throws SynthesiseException
    {
        testSynthesizeHelp("nothing", "nothing");
    }

    @Test
    public void testSynthesize2() throws SynthesiseException
    {
        String program = "preable /// foo\n afterword";
        String correct = "preable edu.rice.cs.caper.bayou.annotations.Evidence.freeform(\"foo\");\n afterword";

        testSynthesizeHelp(program, correct);
    }

    @Test(expected = SynthesiseException.class)
    public void testSynthesizeNoIdent() throws SynthesiseException
    {
        ApiSynthesizerJustReturn inner = new ApiSynthesizerJustReturn();
        new ApiSynthesizerRewriteEvidenceDecorator(inner).synthesise("/// dog,", 1).iterator().next();
    }

    private void testSynthesizeHelp(String program, String correct) throws SynthesiseException
    {
        ApiSynthesizerJustReturn inner = new ApiSynthesizerJustReturn();

        String result = new ApiSynthesizerRewriteEvidenceDecorator(inner).synthesise(program, 1).iterator().next();
        Assert.assertEquals(correct, result);

        result = new ApiSynthesizerRewriteEvidenceDecorator(inner).synthesise(program, 1, 1).iterator().next();
        Assert.assertEquals(correct, result);
    }


    @Test
    public void rewriteEvidenceBluetooth() throws UnexpectedEndOfCharacters, ParseException
    {
        String program =
                "import android.bluetooth.BluetoothAdapter;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestBluetooth {\n" +
                        "\n" +
                        "    /* Get an input stream that can be used to read from\n" +
                        "     * the given blueooth hardware address */\n" +
                        "    void readFromBluetooth(BluetoothAdapter adapter) {\n" +
                        "        // Intersperse code with evidence\n" +
                        "        String address = \"00:43:A8:23:10:F0\";\n" +
                        "\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"getInputStream\"...\n" +
                        "            /// calls:getInputStream\n" +
                        "            // ...on a \"BluetoothSocket\" type\n" +
                        "            /// type:BluetoothSocket\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }\n" +
                        "\n" +
                        "}\n";

        String result = ApiSynthesizerRewriteEvidenceDecorator.rewriteEvidence(program);

        String correct =
                "import android.bluetooth.BluetoothAdapter;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestBluetooth {\n" +
                        "\n" +
                        "    /* Get an input stream that can be used to read from\n" +
                        "     * the given blueooth hardware address */\n" +
                        "    void readFromBluetooth(BluetoothAdapter adapter) {\n" +
                        "        // Intersperse code with evidence\n" +
                        "        String address = \"00:43:A8:23:10:F0\";\n" +
                        "\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"getInputStream\"...\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"getInputStream\");\n" +
                        "            // ...on a \"BluetoothSocket\" type\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.types(\"BluetoothSocket\");\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }\n" +
                        "\n" +
                        "}\n";

        Assert.assertEquals(correct, result);
    }

    @Test
    public void rewriteEvidenceCamera() throws UnexpectedEndOfCharacters, ParseException
    {
        String program =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestCamera {\n" +
                        "\n" +
                        "    /* Start a preview of the camera, by setting the\n" +
                        "     * preview's width and height using the given ints */\n" +
                        "    void preview() {\n" +
                        "        // Intersperse code with evidence\n" +
                        "        int width = 640;\n" +
                        "        int height = 480;\n" +
                        "\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"startPreview\"...\n" +
                        "            /// calls: startPreview\n" +
                        "            // ...and use an \"int\" as argument\n" +
                        "            /// context: int\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        String result = ApiSynthesizerRewriteEvidenceDecorator.rewriteEvidence(program);

        String correct =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestCamera {\n" +
                        "\n" +
                        "    /* Start a preview of the camera, by setting the\n" +
                        "     * preview's width and height using the given ints */\n" +
                        "    void preview() {\n" +
                        "        // Intersperse code with evidence\n" +
                        "        int width = 640;\n" +
                        "        int height = 480;\n" +
                        "\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"startPreview\"...\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"startPreview\");\n" +
                        "            // ...and use an \"int\" as argument\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.context(\"int\");\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        Assert.assertEquals(correct, result);
    }

    @Test
    public void rewriteEvidenceDialog() throws UnexpectedEndOfCharacters, ParseException
    {
        String program =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "import android.content.Context;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestDialog {\n" +
                        "\n" +
                        "    /* Create an alert dialog with the given strings\n" +
                        "     * as content (title and message) in the dialog */\n" +
                        "    void createDialog(Context c) {\n" +
                        "        // Intersperse code with evidence\n" +
                        "        String str1 = \"something here\";\n" +
                        "        String str2 = \"another thing here\";\n" +
                        "\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"setTitle\" and \"setMessage\"...\n" +
                        "            /// calls: setTitle, setMessage\n" +
                        "            // ...on an \"AlertDialog\" type\n" +
                        "            /// type: AlertDialog\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        String result = ApiSynthesizerRewriteEvidenceDecorator.rewriteEvidence(program);

        String correct =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "import android.content.Context;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestDialog {\n" +
                        "\n" +
                        "    /* Create an alert dialog with the given strings\n" +
                        "     * as content (title and message) in the dialog */\n" +
                        "    void createDialog(Context c) {\n" +
                        "        // Intersperse code with evidence\n" +
                        "        String str1 = \"something here\";\n" +
                        "        String str2 = \"another thing here\";\n" +
                        "\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"setTitle\" and \"setMessage\"...\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"setTitle\", \"setMessage\");\n" +
                        "            // ...on an \"AlertDialog\" type\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.types(\"AlertDialog\");\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        Assert.assertEquals(correct, result);
    }

    @Test
    public void rewriteEvidenceIO1() throws UnexpectedEndOfCharacters, ParseException
    {
        String program =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestIO {\n" +
                        "\n" +
                        "    // NOTE: Bayou only supports one synthesis task in a given\n" +
                        "    // program at a time, so please comment out the rest.\n" +
                        "\n" +
                        "    /* Read from a file */\n" +
                        "    void read(String file) {\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"readLine\"\n" +
                        "            /// call: readLine\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        String result = ApiSynthesizerRewriteEvidenceDecorator.rewriteEvidence(program);

        String correct =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestIO {\n" +
                        "\n" +
                        "    // NOTE: Bayou only supports one synthesis task in a given\n" +
                        "    // program at a time, so please comment out the rest.\n" +
                        "\n" +
                        "    /* Read from a file */\n" +
                        "    void read(String file) {\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"readLine\"\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"readLine\");\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        Assert.assertEquals(correct, result);
    }

    @Test
    public void rewriteEvidenceIO2() throws UnexpectedEndOfCharacters, ParseException
    {
        String program =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestIO {\n" +
                        "\n" +
                        "    // NOTE: Bayou only supports one synthesis task in a given\n" +
                        "    // program at a time, so please comment out the rest.\n" +
                        "\n" +
                        "    // Read from a file, more specifically using the\n" +
                        "    // string argument given\n" +
                        "    void read(String file) {\n" +
                        "        {\n" +
                        "            /// calls: readLine context: String\n" +
                        "        }\n" +
                        "    }   \n" +
                        "}\n";

        String result = ApiSynthesizerRewriteEvidenceDecorator.rewriteEvidence(program);

        String correct =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestIO {\n" +
                        "\n" +
                        "    // NOTE: Bayou only supports one synthesis task in a given\n" +
                        "    // program at a time, so please comment out the rest.\n" +
                        "\n" +
                        "    // Read from a file, more specifically using the\n" +
                        "    // string argument given\n" +
                        "    void read(String file) {\n" +
                        "        {\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"readLine\");edu.rice.cs.caper.bayou.annotations.Evidence.context(\"String\");\n" +
                        "        }\n" +
                        "    }   \n" +
                        "}\n";

        Assert.assertEquals(correct, result);
    }

    @Test
    public void rewriteEvidenceIO3() throws UnexpectedEndOfCharacters, ParseException
    {
        String program =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestIO {\n" +
                        "\n" +
                        "    // NOTE: Bayou only supports one synthesis task in a given\n" +
                        "    // program at a time, so please comment out the rest.\n" +
                        "\n" +
                        "    // Read from the file, performing exception handling\n" +
                        "    // properly by printing the stack trace\n" +
                        "    void readWithErrorHandling() {\n" +
                        "        String file;\n" +
                        "        {\n" +
                        "            /// calls: readLine, printStackTrace, close context: String\n" +
                        "        }\n" +
                        "    }   \n" +
                        "}\n";

        String result = ApiSynthesizerRewriteEvidenceDecorator.rewriteEvidence(program);

        String correct =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestIO {\n" +
                        "\n" +
                        "    // NOTE: Bayou only supports one synthesis task in a given\n" +
                        "    // program at a time, so please comment out the rest.\n" +
                        "\n" +
                        "    // Read from the file, performing exception handling\n" +
                        "    // properly by printing the stack trace\n" +
                        "    void readWithErrorHandling() {\n" +
                        "        String file;\n" +
                        "        {\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"readLine\", \"printStackTrace\", \"close\");edu.rice.cs.caper.bayou.annotations.Evidence.context(\"String\");\n" +
                        "        }\n" +
                        "    }   \n" +
                        "}\n";

        Assert.assertEquals(correct, result);
    }

    @Test
    public void rewriteEvidenceSpeech() throws UnexpectedEndOfCharacters, ParseException
    {
        String program =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "import android.content.Context;\n" +
                        "import android.content.Intent;\n" +
                        "import android.speech.RecognitionListener;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestSpeech {\n" +
                        "\n" +
                        "    /* Construct a speech regonizer with the provided listener */\n" +
                        "    void speechRecognition(Context context, Intent intent, RecognitionListener listener) {\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should make API calls on \"SpeechRecognizer\"...\n" +
                        "            /// type: SpeechRecognizer\n" +
                        "            // ...and use a \"Context\" as argument\n" +
                        "            /// context: Context\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        String result = ApiSynthesizerRewriteEvidenceDecorator.rewriteEvidence(program);

        String correct =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "import android.content.Context;\n" +
                        "import android.content.Intent;\n" +
                        "import android.speech.RecognitionListener;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestSpeech {\n" +
                        "\n" +
                        "    /* Construct a speech regonizer with the provided listener */\n" +
                        "    void speechRecognition(Context context, Intent intent, RecognitionListener listener) {\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should make API calls on \"SpeechRecognizer\"...\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.types(\"SpeechRecognizer\");\n" +
                        "            // ...and use a \"Context\" as argument\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.context(\"Context\");\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        Assert.assertEquals(correct, result);
    }

    @Test
    public void rewriteEvidenceWifi() throws UnexpectedEndOfCharacters, ParseException
    {
        String program =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "import android.net.wifi.WifiManager;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestWifi {\n" +
                        "\n" +
                        "    /* Start a wi-fi scan using the given manager */\n" +
                        "    void scan(WifiManager manager) {\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"startScan\"...\n" +
                        "            /// call: startScan\n" +
                        "            // ...on a \"WifiManager\" type\n" +
                        "            /// type: WifiManager\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        String result = ApiSynthesizerRewriteEvidenceDecorator.rewriteEvidence(program);

        String correct =
                "import edu.rice.cs.caper.bayou.annotations.Evidence;\n" +
                        "import android.net.wifi.WifiManager;\n" +
                        "\n" +
                        "// Bayou supports three types of evidence:\n" +
                        "// 1. apicalls - API methods the code should invoke\n" +
                        "// 2. types - datatypes of objects which invoke API methods\n" +
                        "// 3. context - datatypes of variables that the code should use\n" +
                        "\n" +
                        "public class TestWifi {\n" +
                        "\n" +
                        "    /* Start a wi-fi scan using the given manager */\n" +
                        "    void scan(WifiManager manager) {\n" +
                        "        { // Provide evidence within a separate block\n" +
                        "            // Code should call \"startScan\"...\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"startScan\");\n" +
                        "            // ...on a \"WifiManager\" type\n" +
                        "            edu.rice.cs.caper.bayou.annotations.Evidence.types(\"WifiManager\");\n" +
                        "        } // Synthesized code will replace this block\n" +
                        "\n" +
                        "    }   \n" +
                        "\n" +
                        "}\n";

        Assert.assertEquals(correct, result);
    }

    @Test
    public void testMakeEvidenceFromCommentFreeform() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// foo");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.freeform(\"foo\");\n", result);
    }

    @Test
    public void testMakeEvidenceFromCommentFreeform2() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// foo, bar");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.freeform(\"foo\", \"bar\");\n", result);
    }

    @Test
    public void testMakeEvidenceFromCommentCall() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// call: foo");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"foo\");\n", result);
    }

    @Test
    public void testMakeEvidenceFromCommentCalls1() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// calls: foo");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"foo\");\n", result);
    }

    @Test
    public void testMakeEvidenceFromCommentCalls2() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// calls: foo, bar");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.apicalls(\"foo\", \"bar\");\n", result);
    }

    @Test
    public void testMakeEvidenceFromCommentType() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// type: foo");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.types(\"foo\");\n", result);
    }

    @Test
    public void testMakeEvidenceFromCommentTypes1() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// types: foo");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.types(\"foo\");\n", result);
    }

    @Test
    public void testMakeEvidenceFromCommentTypes2() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// types: foo, bar");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.types(\"foo\", \"bar\");\n", result);
    }

    @Test
    public void testMakeEvidenceFromCommentContext() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// context: foo");

        Assert.assertEquals("edu.rice.cs.caper.bayou.annotations.Evidence.context(\"foo\");\n", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMakeEvidenceFromCommentEmpty() throws ParseException
    {
        ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("");
    }

    @Test(expected = ParseException.class)
    public void testMakeEvidenceFromCommentMissingIdent1() throws ParseException
    {
        ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// calls:");
    }

    @Test(expected = ParseException.class)
    public void testMakeEvidenceFromCommentMissingIdent2() throws ParseException
    {
        ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// foo,");
    }

    @Test(expected = ParseException.class)
    public void testMakeEvidenceFromCommentUknownType() throws ParseException
    {
        ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("/// unknown: foo");
    }

    @Test
    public void testMakeEvidenceFromCommentJustSlashes() throws ParseException
    {
        String result = ApiSynthesizerRewriteEvidenceDecorator.makeEvidenceFromComment("///");

        Assert.assertEquals("///", result);
    }


}
