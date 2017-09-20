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
package edu.rice.cs.caper.bayou.core.synthesizer;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SynthesizerTest {

    void testExecute(String test) throws IOException, ParseException
    {
        File projRoot = new File(System.getProperty("user.dir")).getParentFile().getParentFile().getParentFile();
        File srcFolder = new File(projRoot.getAbsolutePath() + File.separator + "src");
        File mainResourcesFolder = new File(srcFolder.getAbsolutePath() + File.separator + "main" + File.separator +
                "resources");

        File artifactsFolder = new File(mainResourcesFolder + File.separator + "artifacts");

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
                File.separator + "synthesizer";


        Synthesizer synthesizer = new Synthesizer();

        String code = new String(Files.readAllBytes(Paths.get(String.format("%s/%s.java", testDir, test))));
        String asts = new String(Files.readAllBytes(Paths.get(String.format("%s/%s.json", testDir, test))));

        Parser parser = new Parser(code, classpath);
        parser.parse();
        List<String> results = synthesizer.execute(parser, asts);

        Assert.assertTrue(results.size() > 0);

        for(String resultProgram : results) {
            Assert.assertTrue(resultProgram.contains("public class")); // some code was synthesized
        }
    }

    @Test
    public void testIO1() throws IOException, ParseException {
        testExecute("TestIO1");
    }

    @Test
    public void testIO2() throws IOException, ParseException {
        testExecute("TestIO2");
    }

    @Test
    public void testUtil() throws IOException, ParseException {
        testExecute("TestUtil");
    }

    @Test
    public void testBluetooth() throws IOException, ParseException {
        testExecute("TestBluetooth");
    }

    @Test
    public void testDialog() throws IOException, ParseException {
        testExecute("TestDialog");
    }

    @Test
    public void testCamera() throws IOException, ParseException {
        testExecute("TestCamera");
    }

    @Test
    public void testWifi() throws IOException, ParseException {
        testExecute("TestWifi");
    }

    @Test
    public void testSpeech() throws IOException, ParseException {
        testExecute("TestSpeech");
    }
    
}
