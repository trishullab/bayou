package edu.rice.cs.caper.bayou.core.synthesizer;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;

import java.util.*;

public class DCEOptimizor extends ASTVisitor {
    // The def variables
    protected Map<String, List<ASTNode> > defs;
    
    // The use variables
    protected Map<String, List<ASTNode> > uses;

    // The eliminated variable declarations
    protected Set<String> eliminatedVars;
    
    public DCEOptimizor() {
	this.defs = new HashMap<String, List<ASTNode> >();
	this.uses = new HashMap<String, List<ASTNode> >();
	this.eliminatedVars = new HashSet<String>();
    }

    // Apply the optimization here
    public Block apply(Block body, DSubTree dAST) {
	// Collect defs and uses
	collectDefUse(body);
	
	// Check if def has potential uses
	ArrayList<String> tempVars = new ArrayList<String>();
	for (String def : defs.keySet()) {
	    List<ASTNode> defVals = defs.get(def);
	    if (defVals.size() == 1) {
		if (uses.get(def) == null) {
		    // No use, then remove this def's corresponding ExpressionStatement from synthesized code block
		    tempVars.add(def);
		}
	    }
	}
	// Clean up the infeasible elimination
	for (String def : tempVars) {
	    List<ASTNode> defVals = defs.get(def);
	    ASTNode defNode = defVals.get(0);

	    if (defNode.getParent() instanceof ExpressionStatement) {
		defNode.getParent().delete();
		this.eliminatedVars.add(def);
	    } else if (hasLegalParent(defNode)) {
		this.eliminatedVars.add(def);
	    }
	}
	// Apply post optimizations to dAST
	dAST.cleanupCatchClauses(this.eliminatedVars);
	
	return body;
    }

    protected boolean hasLegalParent(ASTNode node) {
	node = node.getParent();
	System.out.println("parent type: " + node.getClass().getName());
	while (node != null && (node instanceof ClassInstanceCreation
				|| node instanceof ParenthesizedExpression
				|| node instanceof Assignment)) {
	    node = node.getParent();
	    System.out.println("parent type: " + node.getClass().getName());
	}

	return node != null && node instanceof ExpressionStatement;
    }

    // Collect the def and use variables
    protected void collectDefUse(Block body) {
	body.accept(this);
    }

    public Set<String> getEliminatedVars() {
	return this.eliminatedVars;
    }
    
    @Override
    public boolean visit(Assignment assign) {
	return true;
    }

    @Override
    public boolean visit(SimpleName name) {
	Statement stmt = getParentStatement(name);
	String varName = name.toString(); 

	boolean isDef = false;
	ASTNode parent = name.getParent();
	if (parent instanceof Assignment) {
	    isDef = ((Assignment)parent).getLeftHandSide() == name
		&& ((Assignment)parent).getRightHandSide() instanceof ClassInstanceCreation
		&& parent.getParent() != null;
	    // && parent.getParent() instanceof ExpressionStatement;
	} 
	
	if (varName != null && stmt != null) {
	    if (isDef)
		// Add variable def
		addToMap(varName, name.getParent(), defs);
	    else
		// Add potential use
		addToMap(varName, stmt, uses);
	}
	
	return false;
    }

    @Override
    public boolean visit(QualifiedName name) {
	Statement stmt = getParentStatement(name);
	String varName = name.toString();

	boolean isDef = false;
	ASTNode parent = name.getParent();
	if (parent instanceof Assignment) {
	    isDef = ((Assignment)parent).getLeftHandSide() == name;
	}

	if (varName != null && stmt != null) {
	    if (isDef)
		// Add variable def
		addToMap(varName, stmt, defs);
	    else
		// Add potential use
		addToMap(varName, stmt, uses);
	}
	
	return false;
    }

    @Override
    public boolean visit(ConstructorInvocation constInvoke) {
	System.out.println("check constructor ");
	return true;
    }
    
    // Add variable and its parent to register map
    protected void addToMap(String varName, ASTNode parent, Map<String, List<ASTNode> > varMap) {
	List<ASTNode> values = varMap.get(varName);
	if (values == null) {
	    values = new ArrayList<ASTNode>();
	    varMap.put(varName, values);
	}
	values.add(parent);
    }

    // Get the potential variable name from the given expression
    protected String getVariableName(Expression expr) {
	if (expr instanceof Name) {
	    Name name = (Name)expr;
	    IBinding bind = name.resolveBinding();
	    if (bind != null) {
		if (bind instanceof IVariableBinding) {
		    String varName = ((IVariableBinding)bind).getName();
		    return varName;
		}
	    } 
	}

	return null;
    }
    
    // Get the parent statement
    protected Statement getParentStatement(Expression expr) {
	ASTNode node = expr;
	while (!(node.getParent() instanceof Statement)) {
	    node = node.getParent();
	}

	return (Statement)node.getParent();
    }
}
