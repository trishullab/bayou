package edu.rice.cs.caper.bayou.application.api_synthesis_server;

import org.junit.Assert;
import org.junit.Test;

public class ApiSynthesisStrategyRemoteTensorFlowAstsTests
{
    @Test
    public void rewriteEvidenceTest()
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
                "            /// call:getInputStream\n" +
                "            // ...on a \"BluetoothSocket\" type\n" +
                "            /// type:BluetoothSocket\n" +
                "        } // Synthesized code will replace this block\n" +
                "    }\n" +
                "\n" +
                "}\n";

        String result = ApiSynthesisStrategyRemoteTensorFlowAsts.rewriteEvidence(program);

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
}
