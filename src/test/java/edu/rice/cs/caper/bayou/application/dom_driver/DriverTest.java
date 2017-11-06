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
package edu.rice.cs.caper.bayou.application.dom_driver;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.cli.ParseException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public class DriverTest {

    private void testExecute(String input, String output) throws ParseException, IOException {
        testExecute(input, output, "config");
    }

    private void testExecute(String input, String output, String config)
            throws ParseException, IOException {

        File srcFolder;
        {
            File projRoot = new File(System.getProperty("user.dir")).getParentFile().getParentFile().getParentFile();
            srcFolder = new File(projRoot.getAbsolutePath() + File.separator + "src");
        }

        File artifactsFolder;
        {
            File mainResourcesFolder = new File(srcFolder.getAbsolutePath() + File.separator + "main" + File.separator +
                    "resources");
            artifactsFolder = new File(mainResourcesFolder + File.separator + "artifacts");
        }

        String classpath;
        {
            File classesFolder = new File(artifactsFolder.getAbsolutePath() + File.separator + "classes");
            File androidJar = new File(artifactsFolder.getAbsolutePath() + File.separator + "jar" + File.separator +
                    "android.jar");

            if(!classesFolder.exists())
                throw new IllegalStateException();

            if(!androidJar.exists())
                throw new IllegalStateException();

            classpath = classesFolder.getAbsolutePath() + File.pathSeparator + androidJar.getAbsolutePath();
        }

        String testDir = srcFolder.getAbsolutePath() + File.separator + "test" + File.separator + "resources" +
                File.separator + "driver";

        // run the driver
        File inputFile = new File(String.format("%s/%s.java", testDir, input));
        File outputFile = new File(String.format("%s/%s.json", testDir, output));
        File configFile = new File(String.format("%s/%s.json", testDir, config));
        File tmpFile = new File(String.format("%s/%s-tmp.json", testDir, input));

        String[] args = {"-f", inputFile.getAbsolutePath(),
                         "-c", configFile.getAbsolutePath(),
                         "-o", tmpFile.getAbsolutePath()};
        new Driver(args).execute(classpath);

        // match the output and expected JSON
        String out = new String(Files.readAllBytes(Paths.get(tmpFile.getAbsolutePath())));
        String exp = new String(Files.readAllBytes(Paths.get(outputFile.getAbsolutePath())));

        JsonParser parser = new JsonParser();
        JsonArray expJSON = parser.parse(exp).getAsJsonObject().getAsJsonArray("programs");
        JsonArray outJSON = parser.parse(out).getAsJsonObject().getAsJsonArray("programs");
        Assert.assertTrue(outJSON.size() == expJSON.size());
        for (int i = 0; i < expJSON.size(); i++) {
            JsonObject o = outJSON.get(i).getAsJsonObject();
            JsonObject e = expJSON.get(i).getAsJsonObject();
            Assert.assertTrue(compareAST(e, o));
            Assert.assertTrue(compareSequences(e, o));
            Assert.assertTrue(compareJavadoc(e, o));
        }

        if (!tmpFile.delete())
            System.out.println(tmpFile.getAbsolutePath() + " could not be deleted!");
    }

    private boolean compareAST(JsonObject js1, JsonObject js2) {
        return js1.getAsJsonObject("ast").equals(js2.getAsJsonObject("ast"));
    }

    private boolean compareSequences(JsonObject js1, JsonObject js2) {
        return js1.getAsJsonArray("sequences").equals(js2.getAsJsonArray("sequences"));
    }

    private boolean compareJavadoc(JsonObject js1, JsonObject js2) {
        if (js1.get("javadoc").isJsonNull())
            return js2.get("javadoc").isJsonNull();
        return js1.getAsJsonPrimitive("javadoc").equals(js2.getAsJsonPrimitive("javadoc"));
    }

    @Test
    public void test1() throws ParseException, IOException {
        try {
            testExecute("1f", "1o");
        } catch (NoSuchFileException e) {
            return; // output file must NOT be created for this test
        }
        Assert.assertTrue(false);
    }

    @Test
    public void test2() throws ParseException, IOException {
        testExecute("2f", "2o");
    }

    @Test
    public void test3() throws ParseException, IOException {
        testExecute("3f", "3o");
    }

    @Test
    public void test4() throws ParseException, IOException {
        testExecute("4f", "4o");
    }

    @Test
    public void test5() throws ParseException, IOException {
        testExecute("5f", "5o");
    }

    @Test
    public void test6() throws ParseException, IOException {
        testExecute("6f", "6o");
    }

    @Test
    public void test7() throws ParseException, IOException {
        testExecute("7f", "7o");
    }

    @Test
    public void test8() throws ParseException, IOException {
        testExecute("8f", "8o");
    }

    @Test
    public void test9() throws ParseException, IOException {
        testExecute("9f", "9o");
    }

    @Test
    public void test10() throws ParseException, IOException {
        testExecute("10f", "10o");
    }

    @Test
    public void test11() throws ParseException, IOException {
        testExecute("11f", "11o");
    }

    @Test
    public void test12() throws ParseException, IOException {
        testExecute("12f", "12o");
    }

    @Test
    public void test13() throws ParseException, IOException {
        testExecute("13f", "13o");
    }

    @Test
    public void test14() throws ParseException, IOException {
        testExecute("14f", "14o");
    }

    @Test
    public void test15() throws ParseException, IOException {
        testExecute("15f", "15o");
    }

    @Test
    public void test16() throws ParseException, IOException {
        testExecute("16f", "16o");
    }

    @Test
    public void test17() throws ParseException, IOException {
        testExecute("17f", "17o");
    }

    @Test
    public void test18() throws ParseException, IOException {
        testExecute("18f", "18o");
    }

    @Test
    public void test19() throws ParseException, IOException {
        testExecute("19f", "19o");
    }

    @Test
    public void test20() throws ParseException, IOException {
        testExecute("20f", "20o");
    }

    @Test
    public void test21() throws ParseException, IOException {
        testExecute("21f", "21o");
    }

    @Test
    public void test22() throws ParseException, IOException {
        testExecute("22f", "22o");
    }

    @Test
    public void test23() throws ParseException, IOException {
        testExecute("23f", "23o");
    }

    @Test
    public void test24() throws ParseException, IOException {
        testExecute("24f", "24o");
    }

    @Test
    public void test25() throws ParseException, IOException {
        testExecute("25f", "25o");
    }

    @Test
    public void test26() throws ParseException, IOException {
        testExecute("26f", "26o");
    }

    @Test
    public void test27() throws ParseException, IOException {
        testExecute("27f", "27o");
    }

    @Test
    public void test28() throws ParseException, IOException {
        testExecute("28f", "28o");
    }

    @Test
    public void test29() throws ParseException, IOException {
        testExecute("29f", "29o");
    }

    @Test
    public void test30() throws ParseException, IOException {
        testExecute("30f", "30o");
    }

    @Test
    public void test31() throws ParseException, IOException {
        testExecute("31f", "31o");
    }

    @Test
    public void test32() throws ParseException, IOException {
        testExecute("32f", "32o");
    }

    @Test
    public void test33() throws ParseException, IOException {
        testExecute("33f", "33o");
    }

    @Test
    public void test34() throws ParseException, IOException {
        testExecute("34f", "34o");
    }
}
