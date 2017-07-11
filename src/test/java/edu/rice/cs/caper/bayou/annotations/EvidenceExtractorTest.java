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
package edu.rice.cs.caper.bayou.annotations;


import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import edu.rice.cs.caper.bayou.core.synthesizer.EvidenceExtractor;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class EvidenceExtractorTest
{

    private void testExecute(String test) throws IOException
    {
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
                         File.separator + "synthesizer";

        String code = new String(Files.readAllBytes(Paths.get(String.format("%s/%s.java", testDir, test))));
        String content = new EvidenceExtractor().execute(code, classpath);

        new Gson().fromJson(content, Object.class); // check valid JSON

    }

    @Test
    public void testIO1() throws IOException {
        testExecute("TestIO1");
    }

    @Test
    public void testIO2() throws IOException {
        testExecute("TestIO2");
    }

    @Test
    public void testBluetooth() throws IOException {
        testExecute("TestBluetooth");
    }

    @Test
    public void testDialog() throws IOException {
        testExecute("TestDialog");
    }

    @Test
    public void testCamera() throws IOException {
        testExecute("TestCamera");
    }

    @Test
    public void testWifi() throws IOException {
        testExecute("TestWifi");
    }

    @Test
    public void testSpeech() throws IOException {
        testExecute("TestSpeech");
    }
}
