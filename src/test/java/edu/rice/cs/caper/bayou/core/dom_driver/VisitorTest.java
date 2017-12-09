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
package edu.rice.cs.caper.bayou.core.dom_driver;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.io.IOException;

import edu.rice.cs.caper.bayou.core.dom_driver.Options;
import edu.rice.cs.caper.bayou.core.dom_driver.Visitor;

public class VisitorTest
{
    @Test
    public void testBuildJson() throws IOException
    {
        String source = "import java.io.BufferedReader;\n" +
                "import java.io.InputStreamReader;\n" +
                "import java.io.FileReader;\n" +
                "import java.io.File;\n" +
                "\n" +
                "class Test {\n" +
                "    BufferedReader br;\n" +
                "    public Test(File file) {\n" +
                "        br = new BufferedReader(new FileReader(file));\n" +
                "    }\n" +
                "}";
        String unitName = "Test.java";

        CompilationUnit cu;
        {
            ASTParser parser = ASTParser.newParser(AST.JLS8);

            parser.setSource(source.toCharArray());
            parser.setKind(ASTParser.K_COMPILATION_UNIT);
            parser.setUnitName(unitName);
            parser.setEnvironment(new String[] { "" }, new String[] { "" }, new String[] { "UTF-8" }, true);
            parser.setResolveBindings(true);

            cu = (CompilationUnit) parser.createAST(null);
        }
        Options options = new Options();
        Visitor visitor = new Visitor(cu, options);
        cu.accept(visitor);
        String sketch = visitor.buildJson();

        Assert.assertNotNull(sketch);
    }
}
