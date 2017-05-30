/* Author: Vijay Murali */


/*
 * The DSL is implemented as a subset of the Eclipse AST. The convention
 * is to use the Eclipse AST class name prefixed with D. Every class that
 * wishes to be in the DSL has to provide an inner class Handle that extends
 * the class Handler. The Handle takes an Eclipse AST element and the visitor
 * (through its constructor) and upon invoking the handle() method, returns
 * the corresponding DSL element. The handle() method may return null if
 * the Eclipse AST element does not satisfy the conditions to be in the DSL.
 *
 * Note that if the DSL element is a subclass of DOMExpression or DASTNode
 * it has to be added to the switch cases in the Handles of both these classes.
 */



package edu.rice.cs.caper.lib.bayou.dom_driver;


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

    private CompilationUnit createCompilationUnit() throws IOException {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        File input = new File(options.cmdLine.getOptionValue("input-file"));
        String classpath = System.getenv("CLASSPATH");

        parser.setSource(FileUtils.readFileToString(input, "utf-8").toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setUnitName("Program.java");
        parser.setEnvironment(new String[] { classpath != null? classpath : "" },
                new String[] { "" }, new String[] { "UTF-8" }, true);
        parser.setResolveBindings(true);

        return (CompilationUnit) parser.createAST(null);
    }

    public void execute() throws IOException {
        CompilationUnit cu = createCompilationUnit();
        Visitor visitor = new Visitor(cu, options);
        cu.accept(visitor);
        visitor.output.close();
    }

	public static void main(String args[]) {
        try {
            new Driver(args).execute();
        } catch (ParseException | IOException e) {
            System.out.println("Unexpected exception: " + e.getMessage());
        }
	}
}
