package edu.rice.cs.caper.bayou.application.dom_driver;

import java.util.*;

import edu.rice.cs.caper.bayou.core.dsl.DASTNode;
import edu.rice.cs.caper.bayou.core.dsl.DBranch;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.SwitchStatement;

public class DOMSwitchStatement implements Handler {

    final SwitchStatement statement;

    DSubTree tree = new DSubTree();

    ArrayList<DSubTree> DBodies = new ArrayList<DSubTree>();
    ArrayList<List<DASTNode>> bodies = new ArrayList<List<DASTNode>>();
    ArrayList<Integer> nodeType = new ArrayList<Integer>();


    public DOMSwitchStatement(SwitchStatement statement) {
        this.statement = statement;
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
}


