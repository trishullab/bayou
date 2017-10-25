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
package edu.rice.cs.caper.bayou.application.experiments.program_generation.aml_ast_type_checker;

import edu.rice.cs.caper.bayou.application.dom_driver.DOMMethodDeclaration;
import edu.rice.cs.caper.bayou.core.synthesizer.ParseException;
import edu.rice.cs.caper.bayou.core.synthesizer.Parser;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class AMLASTTypeChecker {

    void typeCheck(String datafile) throws IOException, ParseException {
        List<JSONInputFormat.DataPoint> programs = JSONInputFormat.readData(datafile);

        // setup the classpath
        File projRoot = new File(System.getProperty("user.dir")).getParentFile().getParentFile().getParentFile();
        File srcFolder = new File(projRoot.getAbsolutePath() + File.separator + "src");
        File mainResourcesFolder = new File(srcFolder.getAbsolutePath() + File.separator + "main" + File.separator +
                "resources");

        File artifactsFolder = new File(mainResourcesFolder + File.separator + "artifacts");
        File classesFolder = new File(artifactsFolder.getAbsolutePath() + File.separator + "classes");
        File androidJar = new File(artifactsFolder.getAbsolutePath() + File.separator + "jar" + File.separator +
                "android.jar");

        if(!classesFolder.exists())
            throw new IllegalStateException();

        if(!androidJar.exists())
            throw new IllegalStateException();

        String classpath = classesFolder.getAbsolutePath() + File.pathSeparator + androidJar.getAbsolutePath();

        // compile each program in data
        int totalPrograms = 0, programsWithAtLeastOneTypeSafeAST = 0;
        for (JSONInputFormat.DataPoint program : programs) {
            totalPrograms += 1;
            String aml = program.aml;
            String header = aml.substring(0, aml.indexOf(')') + 1); // includes class {
            for (DOMMethodDeclaration out_aml_ast : program.out_aml_asts) {
                String body = out_aml_ast.toAML();
                String source = header + body + "}"; // for class }
                Parser parser = new Parser(source, classpath);
                try {
                    parser.parse();
                } catch (ParseException e) {
                    continue;
                }
                programsWithAtLeastOneTypeSafeAST += 1;
                break;
            }
        }

        System.out.println("Type-safe ASTs: " + programsWithAtLeastOneTypeSafeAST + "/" + totalPrograms);
    }

    public static void main(String args[]) throws IOException, ParseException {
        if (args.length != 1) {
            System.err.println("Usage: amlASTTypeChecker DATA-out.json (this is an OUTPUT file from inference with the field out_aml_asts)");
            System.exit(1);
        }
        new AMLASTTypeChecker().typeCheck(args[0]);
    }
}
