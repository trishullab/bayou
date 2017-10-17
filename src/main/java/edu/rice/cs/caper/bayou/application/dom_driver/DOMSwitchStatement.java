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

import java.util.*;

import com.google.gson.annotations.Expose;
import edu.rice.cs.caper.bayou.core.dsl.DASTNode;
import edu.rice.cs.caper.bayou.core.dsl.DBranch;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;

public class DOMSwitchStatement extends DOMStatement implements Handler {

    final SwitchStatement statement;

    DSubTree tree = new DSubTree();

    ArrayList<DSubTree> DBodies = new ArrayList<DSubTree>();
    ArrayList<List<DASTNode>> bodies = new ArrayList<List<DASTNode>>();
    ArrayList<Integer> nodeType = new ArrayList<Integer>();

    @Expose
    final String node = "DOMSwitchStatement";

    @Expose
    final DOMExpression _expression;

    @Expose
    final List<DOMStatement> _statements;

    public DOMSwitchStatement(SwitchStatement statement) {
        this.statement = statement;
        this._expression = new DOMExpression(statement.getExpression()).handleAML();
        this._statements = new ArrayList<>();
        for (Object o : statement.statements())
            this._statements.add(new DOMStatement((Statement) o).handleAML());
    }


    private List<DASTNode> BuildTree(DSubTree Sexpr, int itPos) {
	    List<DASTNode> bodyPrev =  new ArrayList();
	    List<DASTNode> bodyNext =  new ArrayList();
	    List<DASTNode> caseNodes =  new ArrayList();
	    for (int it1=itPos; it1<bodies.size(); it1++){
		    DSubTree Dbody = DBodies.get(it1);
		    int typePrev = nodeType.get(it1);
		    if (typePrev == 49){//checks for 'case' statement 
			    bodyNext = BuildTree(Sexpr, it1+1);
			    DASTNode caseNode = new DBranch(Sexpr.getNodesAsCalls(), bodyPrev, bodyNext );
			    caseNodes.add(caseNode);
			    return caseNodes;
		    } else {
			    bodyPrev.addAll(bodies.get(it1));
		    }

	    }

	    return bodyPrev;
    }


    @Override
    public DSubTree handle() {




        DSubTree Sexpr = new DOMExpression(statement.getExpression()).handle();
	boolean branch = Sexpr.isValid();

	for (Object o : statement.statements()) {
		int type = ((Statement) o).getNodeType();
		nodeType.add(type);
		DSubTree body = new DOMStatement((Statement) o).handle();
		bodies.add(body.getNodes());
		DBodies.add(body);
		if (type != 49)//excludes 'case' statement 
			branch |= body.isValid();
	}


	if (branch){

	        List<DASTNode> switchNodes =  new ArrayList();
		switchNodes = BuildTree(Sexpr, 1);
		tree.addNode(switchNodes.get(0));
	} else {
		// only one  will add nodes, the rest will add nothing
		tree.addNodes(Sexpr.getNodes());
                for (Iterator<Object> iter = statement.statements().iterator(); iter.hasNext(); ) {
			Object o = iter.next();
			DSubTree body = new DOMStatement((Statement) o).handle();
			tree.addNodes(body.getNodes());
		}
	}


        return tree;
    }

    @Override
    public DOMSwitchStatement handleAML() {
        return this;
    }
}


