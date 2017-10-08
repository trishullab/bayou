package edu.rice.cs.caper.bayou.application.dom_driver;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

/**
 * This class extracts the Abstract Skeleton Feature
 */
public class CFGFeature extends SourceFeature {
    private List<CFGNode> nodes;
    private CFG cfg;
    private String dot;
    private static final Logger logger = LogManager.getLogger(CFGFeature.class.getName());
    private Map<ASTNode, Integer> idmap;

    private void cfg_to_dot() {
        this.idmap = new HashMap<>();
        this.nodes = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        StringBuilder result = new StringBuilder();
        result.append("digraph G {\n");

        Queue<CFGNode> queue = new LinkedList<>();
        queue.add(this.cfg.start);

        while(!queue.isEmpty()) {
            CFGNode front = queue.poll();
            int id;

            if(!this.idmap.containsKey(front.node)) {
                id = this.idmap.size();
                assert(id == this.nodes.size());
                this.nodes.add(front);

                this.idmap.put(front.node, id);
                String label = front.node.toString().replace("\n", "\t").replace("\"", "\\\"");
                result.append("  node_" + id + " [label = \"" + label + "\"];\n");
            } else {
                id = this.idmap.get(front.node);
            }
            if(visited.contains(id)) {
                continue;
            }
            visited.add(id);

            for(CFGNode c : front.children) {
                int cid;

                if(!this.idmap.containsKey(c.node)) {
                    cid = this.idmap.size();
                    assert(cid == this.nodes.size());
                    this.nodes.add(c);
                    this.idmap.put(c.node, cid);
                    String label = c.node.toString().replace("\n", "\t").replace("\"", "\\\"");
                    result.append("  node_" + cid + " [label = \"" + label + "\"];\n");
                } else {
                    cid = this.idmap.get(c.node);
                }

                result.append("  node_" + id + " -> " + "node_" + cid + ";\n");
                queue.add(c);
            }
        }

        result.append("}\n");
        this.dot = result.toString();
    }

    private int get_id(ASTNode node) {
        if(this.idmap.containsKey(node)) {
            return this.idmap.get(node);
        }
        int id = this.idmap.size();
        this.idmap.put(node, id);
        return id;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CFGFeature &&
                ((CFGFeature) other).cfg.equals(this.cfg) &&
                (((CFGFeature) other).nodes.equals(this.nodes)) &&
                super.equals(other);
    }

    private void handle_while(CFG graph, WhileStatement wstmt, MethodDeclaration method) {
        CFG wcfg = new CFG();
        CFGNode test = new CFGNode(wstmt.getExpression());
        wcfg.add_start(test);

        if(wstmt.getBody().getNodeType() == ASTNode.BLOCK) {
            List<Statement> stmt_list = ((Block) wstmt.getBody()).statements();
            CFG g = this.gen_cfg(stmt_list, method);
            wcfg.add_cfg(g);
            wcfg.end.children.add(test);
        } else {
            CFGNode n = new CFGNode(wstmt.getBody());
            wcfg.add_end(n);
            n.children.add(test);
        }
        graph.add_cfg(wcfg);
    }

    private void handle_dowhile(CFG graph, DoStatement dostmt, MethodDeclaration method) {
        CFG dcfg = new CFG();
        CFGNode test = new CFGNode(dostmt.getExpression());

        if(dostmt.getBody().getNodeType() == ASTNode.BLOCK) {
            List<Statement> stmt_list = ((Block) dostmt.getBody()).statements();
            CFG g = this.gen_cfg(stmt_list, method);
            dcfg.add_cfg(g);
            dcfg.add_end(test);
            dcfg.end.children.add(dcfg.start);
        } else {
            CFGNode n = new CFGNode(dostmt.getBody());
            dcfg.add_end(n);
            dcfg.add_end(test);
            dcfg.end.children.add(dcfg.start);
        }
        graph.add_cfg(dcfg);
    }

    private void handle_cond(CFG graph, IfStatement ifs, MethodDeclaration method) {
        CFG if_cfg = new CFG();
        CFGNode test = new CFGNode(ifs.getExpression());

        // then branch
        CFG then_cfg;
        if(ifs.getThenStatement().getNodeType() == ASTNode.BLOCK) {
            List<Statement> stmt_list = ((Block) ifs.getThenStatement()).statements();
            then_cfg = this.gen_cfg(stmt_list, method);
        } else {
            then_cfg = new CFG();
            then_cfg.add_end(new CFGNode(ifs.getThenStatement()));
        }

        if(ifs.getElseStatement() == null) {
            if_cfg.add_start(test);
            if_cfg.add_cfg(then_cfg);
            ASTNode empty_stmt = method.getAST().newEmptyStatement();
            CFGNode joint_node = new CFGNode(empty_stmt);
            test.children.add(joint_node);
            if_cfg.end.children.add(joint_node);
            if_cfg.end = joint_node;
        } else {

            // handle the else branch
            CFG else_cfg;
            if(ifs.getElseStatement().getNodeType() == ASTNode.BLOCK) {
                List<Statement> stmt_list = ((Block) ifs.getElseStatement()).statements();
                else_cfg = this.gen_cfg(stmt_list, method);
            } else {
                else_cfg = new CFG();
                else_cfg.add_end(new CFGNode(ifs.getElseStatement()));
            }

            test.children.add(then_cfg.start);
            test.children.add(else_cfg.start);
            if_cfg.add_start(test);

            ASTNode empty_stmt = method.getAST().newEmptyStatement();
            CFGNode joint_node = new CFGNode(empty_stmt);
            then_cfg.add_end(joint_node);
            else_cfg.add_end(joint_node);
            if_cfg.end = joint_node;
        }

        graph.add_cfg(if_cfg);
    }

    /**
     * Convert a switch statement into an if statement for generating CFG
     */
    private Statement switch_to_if(List<Statement> ss, int index, Expression test, MethodDeclaration method) {
        assert(ss.get(index).getNodeType() == ASTNode.SWITCH_CASE);
        SwitchCase scase = (SwitchCase) ss.get(index);
        Block b = method.getAST().newBlock();

        if(scase.isDefault()) {
            // default. Get the rest of the statements
            b.statements().addAll(ss.subList(index + 1, ss.size()));
            return b;
        } else {
            IfStatement new_if = method.getAST().newIfStatement();

            // set up the condition
            InfixExpression test_expr = new_if.getAST().newInfixExpression();
            test_expr.setLeftOperand(test);
            test_expr.setOperator(InfixExpression.Operator.EQUALS);
            test_expr.setRightOperand(scase.getExpression());
            new_if.setExpression(test_expr);

            int i = index + 1;
            while(i < ss.size() && ss.get(i).getNodeType() != ASTNode.SWITCH_CASE) {
                b.statements().add(ss.get(i));
            }

            new_if.setThenStatement(b);
            if(i < ss.size()) {
                new_if.setElseStatement(switch_to_if(ss, i, test, method));
            }
            return new_if;
        }
    }

    /**
     * Condense all the basic blocks and generate a control flow graph out of the given method
     */
    private CFG gen_cfg(List<Statement> slist, MethodDeclaration method) {
        CFG graph = new CFG();
        WhileStatement wstmt;
        List<Statement> new_body;
        Block wbody;
        for(Statement stmt : slist) {
            switch(stmt.getNodeType()) {
                case ASTNode.WHILE_STATEMENT:
                    handle_while(graph, (WhileStatement) stmt, method);
                    break;
                case ASTNode.DO_STATEMENT:
                    handle_dowhile(graph, (DoStatement) stmt, method);
                    break;
                case ASTNode.ENHANCED_FOR_STATEMENT:
                    EnhancedForStatement efstmt = (EnhancedForStatement) stmt;
                    wstmt = method.getAST().newWhileStatement();

                    InfixExpression test_expr = wstmt.getAST().newInfixExpression();
                    test_expr.setLeftOperand((Expression) ASTNode.copySubtree(wstmt.getAST(), efstmt.getParameter().getName()));
                    test_expr.setOperator(InfixExpression.Operator.EQUALS);
                    test_expr.setRightOperand(wstmt.getAST().newNullLiteral());
                    wstmt.setExpression((Expression) ASTNode.copySubtree(wstmt.getAST(), test_expr));

                    new_body = new ArrayList<>();

                    // create the variable and ignore the init. We are just generating CFG. No need to preserve
                    // semantic
                    VariableDeclarationFragment vdfrag = wstmt.getAST().newVariableDeclarationFragment();
                    vdfrag.setName((SimpleName) ASTNode.copySubtree(wstmt.getAST(), efstmt.getParameter().getName()));
                    VariableDeclarationStatement vstmt = wstmt.getAST().newVariableDeclarationStatement(vdfrag);
                    new_body.add(vstmt);

                    // add the rest of the body
                    if(efstmt.getBody().getNodeType() == ASTNode.BLOCK) {
                        List copied_stmt = ASTNode.copySubtrees(wstmt.getAST(), ((Block) efstmt.getBody()).statements());
                        new_body.addAll(copied_stmt);
                    } else {
                        new_body.add((Statement) ASTNode.copySubtree(wstmt.getAST(), efstmt.getBody()));
                    }

                    wbody = wstmt.getAST().newBlock();
                    wbody.statements().addAll(new_body);
                    // set the new body
                    wstmt.setBody(wbody);

                    handle_while(graph, wstmt, method);
                    break;
                case ASTNode.FOR_STATEMENT:
                    ForStatement fstmt = (ForStatement) stmt;
                    List<Expression> init_list = fstmt.initializers();
                    List<Expression> update_list = fstmt.updaters();

                    // contruct a while statement from this for statement
                    wstmt = method.getAST().newWhileStatement();

                    // add the init into the CFG
                    for(Expression init : init_list) {
                        System.out.println(init.toString());
                        Expression copied_init = (Expression) ASTNode.copySubtree(wstmt.getAST(), init);
                        ExpressionStatement init_stmt = wstmt.getAST().newExpressionStatement(copied_init);
                        graph.add_end(new CFGNode(init_stmt));
                    }

                    // construct a while statement
                    new_body = new ArrayList<>();

                    // handle the body and the updates first
                    if(fstmt.getBody().getNodeType() == ASTNode.BLOCK) {
                        List copied_stmts = ASTNode.copySubtrees(wstmt.getAST(), ((Block) fstmt.getBody()).statements());
                        new_body.addAll(copied_stmts);
                    } else {
                        new_body.add((Statement) ASTNode.copySubtree(wstmt.getAST(), fstmt.getBody()));
                    }
                    // add the updates into the for body
                    for(Expression u : update_list) {
                        Expression copied_u = (Expression) ASTNode.copySubtree(wstmt.getAST(), u);
                        ExpressionStatement update_stmt = wstmt.getAST().newExpressionStatement(copied_u);
                        new_body.add(update_stmt);
                    }

                    wbody = wstmt.getAST().newBlock();
                    wbody.statements().addAll(new_body);
                    // set the test expressions
                    wstmt.setExpression((Expression) ASTNode.copySubtree(wstmt.getAST(), fstmt.getExpression()));

                    // set the new body
                    wstmt.setBody(wbody);

                    handle_while(graph, wstmt, method);
                    break;
                case ASTNode.IF_STATEMENT:
                    handle_cond(graph, ((IfStatement) stmt), method);
                    break;
                case ASTNode.SWITCH_STATEMENT:
                    SwitchStatement sstmt = (SwitchStatement) stmt;
                    IfStatement ifs = (IfStatement) switch_to_if(sstmt.statements(), 0, sstmt.getExpression(), method);
                    handle_cond(graph, ifs, method);
                    break;
                default:
                    graph.add_end(new CFGNode(stmt));
                    break;
            }
        }
        return graph;
    }

    private int gen_subgraph_helper(CFGNode start, int k, boolean bfs) {
        Map<ASTNode, Integer> visited = new HashMap<>();
        boolean[][] mat = new boolean[k][k];
        Deque<CFGNode> deq = new LinkedList<>();
        deq.add(start);

        while(!deq.isEmpty()) {
            CFGNode front;
            if(bfs) {
                front = deq.poll();
            } else {
                front = deq.pop();
            }
            if(visited.containsKey(front.node) && visited.get(front.node) < 0) {
                continue;
            }

            int id;
            if(visited.containsKey(front.node)) {
                id = visited.get(front.node);
            } else {
                id = visited.size();
            }
            visited.put(front.node, -id);
            if(Math.abs(id) >= k) {
                continue;
            }

            for(CFGNode c : front.children) {
                int cid;
                if(visited.containsKey(c.node)) {
                    cid = visited.get(c.node);
                } else {
                    cid = visited.size();
                    visited.put(c.node, cid);
                }
                if(Math.abs(cid) < k) {
                    mat[Math.abs(id)][Math.abs(cid)] = true;
                    if(bfs) {
                        deq.add(c);
                    } else {
                        deq.push(c);
                    }
                }
            }
        }
        int result = 0;

        for(int i = 0; i < k; ++i) {
            for(int j = 0; j < k; ++j) {
                int r;
                if(mat[i][j]) {
                    r = 1;
                } else {
                    r = 0;
                }
                System.out.print(r + " ");
            }
            System.out.println();
        }
        System.out.println();

        Iterator it = visited.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            System.out.println(pair.getKey().toString().replace("\n", " ") + " : " + pair.getValue());
        }

        for(int i = 0; i < k; ++i) {
            for(int j = 0; j < k; ++j) {
                if(mat[i][j]) {
                    result |= (1 << i * j + j);
                }
            }
        }
        return result;
    }

    /**
     * Generate a subgraph of size k starting at node_id using bfs
     */
    public Multiset<Integer> gen_subgraph(int k, boolean bfs) {
        Multiset<Integer> result = HashMultiset.create();
        CFGFeature.logger.debug("Generating subgraphs using BFS with k = {} ...", k);
        if(this.nodes.size() == 0) {
            CFGFeature.logger.debug("Empty body.");
            return result;
        }
        for (CFGNode node : this.nodes) {
            System.out.println(node.node.toString());
            int code = this.gen_subgraph_helper(node, k, bfs);
            System.out.println("code : " + code + "\n\n");
            result.add(code);
        }

        CFGFeature.logger.debug("Done generating CFG feature.");
        return result;
    }

    /**
     * Generate a list of CFGs. We don't generate graphs with nodes larger than k
     */
    public CFGFeature(MethodDeclaration input) {
        CFGFeature.logger.debug("Creating CFG feature object...");
        this.cfg = this.gen_cfg(input.getBody().statements(), input);
        this.cfg_to_dot();
        System.out.println(this.dot);
    }

    static class CFGNode {
        ASTNode node;
        List<CFGNode> children;

        CFGNode(ASTNode n) {
            this.node = n;
            this.children = new ArrayList<>();
        }
    }

    static class CFG {
        CFGNode start, end;

        CFG() {
            this.start = this.end = null;
        }

        void add_cfg(CFG c) {
            if(this.start == null || this.end == null) {
                this.start = c.start;
                this.end = c.end;
            } else {
                this.end.children.add(c.start);
                this.end = c.end;
            }
        }

        void add_end(CFGNode n) {
            if(this.start == null || this.end == null) {
                this.start = this.end = n;
            } else {
                this.end.children.add(n);
                this.end = n;
            }
        }

        void add_start(CFGNode n) {
            if(this.start == null || this.end == null) {
                this.start = this.end = n;
            } else {
                n.children.add(this.start);
                this.start = n;
            }
        }
    }

}
