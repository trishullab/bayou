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

import com.google.common.collect.Multiset;
import edu.rice.cs.caper.bayou.core.dom_driver.CFGFeature;
import edu.rice.cs.caper.bayou.core.dom_driver.DecoratedSkeletonFeature;
import org.apache.commons.cli.ParseException;
import org.eclipse.jdt.core.dom.*;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FeatureTest {

    //read file content into a string
    public static String read_file(String filePath) throws IOException {
        StringBuilder fileData = new StringBuilder(1000);
        BufferedReader reader = new BufferedReader(new FileReader(filePath));

        char[] buf = new char[10];
        int numRead;
        while ((numRead = reader.read(buf)) != -1) {
            // System.out.println(numRead);
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
            buf = new char[1024];
        }

        reader.close();

        return fileData.toString();
    }

    @Test
    public void testcfg() throws ParseException, IOException {
        File srcFolder;
        {
            File projRoot = new File(System.getProperty("user.dir")).getParentFile().getParentFile().getParentFile();
            srcFolder = new File(projRoot.getAbsolutePath() + File.separator + "src");
        }

        String testDir = srcFolder.getAbsolutePath() + File.separator + "test" + File.separator + "resources" +
            File.separator + "driver";

        String test_filename = "skeleton1.java";
        String src = read_file(testDir + File.separator + test_filename);

        ASTParser parser = ASTParser.newParser(AST.JLS3);
        Map options = new HashMap();
        parser.setCompilerOptions(options);
        parser.setSource(src.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        cu.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration method) {
                CFGFeature feature = new CFGFeature(method);
                Multiset<Integer> set = feature.gen_subgraph(4, true);
                System.out.println(set.toString());

                DecoratedSkeletonFeature sf = new DecoratedSkeletonFeature(method);
                System.out.println(sf.toString());
                return false;
            }
        });
    }
}