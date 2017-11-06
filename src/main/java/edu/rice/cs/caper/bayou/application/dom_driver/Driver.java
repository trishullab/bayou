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

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;
import java.io.IOException;

public class Driver {

    Options options;

    public Driver(String args[]) throws ParseException, IOException {
        this.options = new Options(args);
    }

    private CompilationUnit createCompilationUnit(String classpath) throws IOException {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        File input = new File(options.cmdLine.getOptionValue("input-file"));

        parser.setSource(FileUtils.readFileToString(input, "utf-8").toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { classpath != null? classpath : "" },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);

        return (CompilationUnit) parser.createAST(null);
    }

    public void execute(String classpath) throws IOException {
        CompilationUnit cu = createCompilationUnit(classpath);
        Visitor visitor = new Visitor(cu, options);
        cu.accept(visitor);
        visitor.printJson();
    }

	public static void main(String args[]) {
        try {
            String classpath = System.getenv("CLASSPATH");
            new Driver(args).execute(classpath);
        } catch (ParseException | IOException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
        }
	}
}
