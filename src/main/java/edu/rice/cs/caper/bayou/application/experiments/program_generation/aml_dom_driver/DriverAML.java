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

package edu.rice.cs.caper.bayou.application.experiments.program_generation.aml_dom_driver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import edu.rice.cs.caper.bayou.application.dom_driver.DOMMethodDeclaration;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DriverAML extends ASTVisitor {

    DOMMethodDeclaration amlAST;
    String classpath;

    class Data {
        List<DataPoint> programs;
    }

    class DataPoint {
        String aml;
    }

    DriverAML(String classpath) {
        this.classpath = classpath;
    }

    DOMMethodDeclaration execute(String source) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);

        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        Map<String, String> options = JavaCore.getOptions();
        options.put("org.eclipse.jdt.core.compiler.source", "1.8");
        parser.setCompilerOptions(options);
        parser.setEnvironment(new String[] { classpath != null? classpath : "" },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);

        CompilationUnit cu = (CompilationUnit) parser.createAST(null);
        List<IProblem> problems = Arrays.stream(cu.getProblems()).filter(p ->
                p.isError() &&
                        p.getID() != IProblem.PublicClassMustMatchFileName && // we use "Program.java"
                        p.getID() != IProblem.ParameterMismatch // Evidence varargs
        ).collect(Collectors.toList());
        if (problems.size() > 0)
            throw new IllegalStateException("Parse error in program: " + source);

        amlAST = null;
        cu.accept(this);
        return amlAST;
    }

    @Override
    public boolean visit(MethodDeclaration method) {
        if (amlAST != null)
            throw new IllegalStateException("AML AST was already created?!");
        amlAST = new DOMMethodDeclaration(method).handleAML();
        return false;
    }

    public static void main(String args[]) throws IOException {

        if (args.length != 2) {
            System.out.println("Usage: driverAML DATA-aml.json DATA-output.json");
            System.exit(1);
        }

        // read the data
        System.out.println("Reading data..."); System.out.flush();
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        String s = new String(Files.readAllBytes(Paths.get(args[0])));
        Data js = gson.fromJson(s, Data.class);

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

        // run the driver
        System.out.println("Running driver..."); System.out.flush();
        DriverAML driver = new DriverAML(classpath);
        List<DOMMethodDeclaration> programs = new ArrayList<>();
        int i = 0;
        for (DataPoint program : js.programs) {
            i += 1;
            try {
                if (program.aml.equals("ERROR")) {
                    programs.add(null);
                    System.out.println(String.format("ERROR %d/%d", i, js.programs.size()));
                } else {
                    programs.add(driver.execute(program.aml));
                    System.out.println(String.format("done %d/%d", i, js.programs.size()));
                }
            } catch (RuntimeException e) {
                programs.add(null);
                e.printStackTrace();
                System.out.println(String.format("ERROR1 %d/%d", i, js.programs.size()));
            }
        }

        // write to output file
        gson = new GsonBuilder().setPrettyPrinting().serializeNulls()
                .excludeFieldsWithoutExposeAnnotation()
                .create();
        System.out.println("Writing to output file..."); System.out.flush();
        BufferedWriter bw = new BufferedWriter(new FileWriter(args[1]));
        bw.write("{ \"programs\": " + gson.toJson(programs) + "}");
        bw.close();
    }
}
