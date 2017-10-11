package edu.rice.cs.caper.bayou.application.dom_driver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class extracts the Abstract Skeleton Feature
 */
public class SkeletonFeature extends SourceFeature {
    private static final String FEATURE_NAME = "Skeleton";

    private SkeletonNode skeleton;
    private static final Logger logger = LogManager.getLogger(SkeletonFeature.class.getName());

    // this is used to extract the skeleton
    static private SExprScanner sexpr_scanner;

    @Override
    public boolean equals(Object other) {
        return other instanceof SkeletonFeature &&
                ((SkeletonFeature) other).skeleton.equals(this.skeleton) &&
                super.equals(other);
    }

    @Override
    public String toString() {
        return this.skeleton.toString();
    }

    /**
     * Test whether the skeleton node is interesting
     */
    static private boolean is_interesting(SkeletonNode n) {
        switch (n.TYPE) {
            case SkeletonNode.COND : return true;
            case SkeletonNode.LOOP : return true;
            case SkeletonNode.OP : return true;
            case SkeletonNode.SEQ :
                return ((Seq) n).children.stream().anyMatch(SkeletonFeature::is_interesting);
            default: return false;
        }
    }

    /**
     * Generate a Sequence skeleton node from a list of java statements
     */
    static private Seq gen_seq(List<Statement> l) {
        return new Seq(l.stream()
                .map(SkeletonFeature::gen_skeleton)
                .filter(SkeletonFeature::is_interesting)
                .collect(Collectors.toList()));
    }

    /**
     * This is the main function for generating a skeleton node given a java statement. It only consider certain
     * statements. It doesn't consider statements such as synchronization or exceptions.
     */
    static private SkeletonNode gen_skeleton(Statement s) {
        SkeletonFeature.logger.debug("Looking at statement '{}'", s.toString());
        if(s.getNodeType() == ASTNode.BLOCK) {
            SkeletonFeature.logger.debug("This is a block.");
            Seq node = SkeletonFeature.gen_seq(((Block) s).statements());
            SkeletonFeature.logger.debug("node = {}", node.toString());
            if(node.children.isEmpty()) {
                SkeletonFeature.logger.debug("Empty sequence.");
                return Empty.ONLY;
            } else {
                SkeletonFeature.logger.debug("Non-empty sequence.");
                return node;
            }
        } else if(s.getNodeType() == ASTNode.DO_STATEMENT) {
            SkeletonFeature.logger.debug("This is a do statement.");
            DoStatement dstmt = (DoStatement) s;
            List<SkeletonNode> children = new ArrayList<>();

            // check the body
            children.add(SkeletonFeature.gen_skeleton(dstmt.getBody()));
            children.addAll(SkeletonFeature.get_op(dstmt.getExpression()));

            SkeletonFeature.logger.debug("children = {}", children.toString());

            // filter by interestingness
            List<SkeletonNode> valid_children = children
                .stream()
                .filter(SkeletonFeature::is_interesting)
                .collect(Collectors.toList());
            SkeletonFeature.logger.debug("interesting children = {}", valid_children);

            if(valid_children.isEmpty()) {
                valid_children.add(Empty.ONLY);
            }
            return new Loop(valid_children);
        } else if(s.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT) {
            SkeletonFeature.logger.debug("This is an enhanced for statement.");
            SkeletonNode child = SkeletonFeature.gen_skeleton(((EnhancedForStatement) s).getBody());
            SkeletonFeature.logger.debug("child = {}", child.toString());

            List<SkeletonNode> c = new ArrayList<>();
            if(SkeletonFeature.is_interesting(child)) {
                SkeletonFeature.logger.debug("Interesting child.");
                c.add(child);
            } else {
                SkeletonFeature.logger.debug("No interesting child.");
                c.add(Empty.ONLY);
            }

            return new Loop(c);
        } else if(s.getNodeType() == ASTNode.FOR_STATEMENT) {
            SkeletonFeature.logger.debug("This is a for statement.");
            ForStatement fstmt = (ForStatement) s;
            List<SkeletonNode> children = new ArrayList<>();

            // check the inits and updates
            for(int i = 0; i < fstmt.initializers().size(); ++i) {
                children.addAll(SkeletonFeature.get_op((ASTNode) fstmt.initializers().get(i)));
            }
            children.add(SkeletonFeature.gen_skeleton(((ForStatement) s).getBody()));
            for(int i = 0; i < fstmt.updaters().size(); ++i) {
                children.addAll(SkeletonFeature.get_op((ASTNode) fstmt.updaters().get(i)));
            }

            SkeletonFeature.logger.debug("children = {}", children.toString());

            // filter by interestingness
            List<SkeletonNode> valid_children = children
                .stream()
                .filter(SkeletonFeature::is_interesting)
                .collect(Collectors.toList());
            SkeletonFeature.logger.debug("interesting children = {}", valid_children);

            if(valid_children.isEmpty()) {
                valid_children.add(Empty.ONLY);
            }
            return new Loop(valid_children);
        } else if(s.getNodeType() == ASTNode.IF_STATEMENT) {
            SkeletonFeature.logger.debug("This is an if statement.");
            IfStatement ifs = (IfStatement) s;
            List<SkeletonNode> children = new ArrayList<>();

            // check the test expression
            children.addAll(SkeletonFeature.get_op(ifs.getExpression()));

            // check then branch
            SkeletonFeature.logger.debug("Looking at then branch.");
            SkeletonNode then = SkeletonFeature.gen_skeleton(ifs.getThenStatement());
            SkeletonFeature.logger.debug("then = {}", then.toString());
            if(SkeletonFeature.is_interesting(then)) {
                SkeletonFeature.logger.debug("Then branch is interesting.");
                children.add(then);
            }

            // check else branch
            if(ifs.getElseStatement() != null) {
                SkeletonFeature.logger.debug("Looking at else branch.");
                SkeletonNode orelse = SkeletonFeature.gen_skeleton(ifs.getElseStatement());
                SkeletonFeature.logger.debug("orelse = {}", orelse.toString());
                if(SkeletonFeature.is_interesting(orelse)) {
                    SkeletonFeature.logger.debug("Else branch is interesting.");
                    children.add(orelse);
                }
            }

            // if we have nothing, add an empty node
            SkeletonFeature.logger.debug("We have {} children.", children.size());
            if(children.isEmpty()) {
                children.add(Empty.ONLY);
            }

            return new Cond(children);
        } else if(s.getNodeType() == ASTNode.LABELED_STATEMENT) {
            SkeletonFeature.logger.debug("This is a labeled statement.");
            return SkeletonFeature.gen_skeleton(((LabeledStatement) s).getBody());
        } else if(s.getNodeType() == ASTNode.SWITCH_CASE) {
            SkeletonFeature.logger.warn("Ignoring switch case statement.");
            return Empty.ONLY;
        } else if(s.getNodeType() == ASTNode.SWITCH_STATEMENT) {
            SkeletonFeature.logger.warn("Ignoring switch statement.");
            return Empty.ONLY;
        } else if(s.getNodeType() == ASTNode.SYNCHRONIZED_STATEMENT) {
            SkeletonFeature.logger.warn("Ignoring synchronized statement.");
            return Empty.ONLY;
        } else if(s.getNodeType() == ASTNode.TRY_STATEMENT) {
            SkeletonFeature.logger.warn("Ignoring try statement.");
            return Empty.ONLY;
        } else if(s.getNodeType() == ASTNode.WHILE_STATEMENT) {
            SkeletonFeature.logger.debug("This is a while statement.");

            // get all children together
            List<SkeletonNode> children = new ArrayList<>();
            WhileStatement wstmt = (WhileStatement) s;
            children.addAll(SkeletonFeature.get_op(wstmt.getExpression()));
            children.add(SkeletonFeature.gen_skeleton(((WhileStatement) s).getBody()));
            SkeletonFeature.logger.debug("children = {}", children);

            // filter by interestingness
            List<SkeletonNode> valid_children = children
                .stream()
                .filter(SkeletonFeature::is_interesting)
                .collect(Collectors.toList());
            SkeletonFeature.logger.debug("interesting children = {}", valid_children);

            if(valid_children.isEmpty()) {
                valid_children.add(Empty.ONLY);
            }
            return new Loop(valid_children);
        }
        return Empty.ONLY;
    }

    /**
     * Get all the interesting operations in this ast node
     */
    private static List<SkeletonNode> get_op(ASTNode node) {
        List<SkeletonNode> result = new ArrayList<>();
        node.accept(new ASTVisitor() {
            @Override
            public boolean visit(InfixExpression expr) {
                expr.getLeftOperand().accept(this);
                if(!expr.getOperator().equals(InfixExpression.Operator.EQUALS)) {
                    result.add(new Op(expr.getOperator().toString()));
                }
                expr.getRightOperand().accept(this);
                return true;
            }

            @Override
            public boolean visit(PrefixExpression expr) {
                result.add(new Op(expr.getOperator().toString()));
                expr.getOperand().accept(this);
                return true;
            }

            @Override
            public boolean visit(PostfixExpression expr) {
                result.add(new Op(expr.getOperator().toString()));
                expr.getOperand().accept(this);
                return super.visit(expr);
            }
        });
        return result;
    }

    public SkeletonFeature(MethodDeclaration input) {
        SkeletonFeature.logger.debug("Generating skeleton feature...");
        Statement s = input.getBody();
        if(s == null) {
            this.skeleton = Empty.ONLY;
        } else {
            this.skeleton = SkeletonFeature.gen_skeleton(s);
        }
        SkeletonFeature.logger.debug("Done generating skeleton feature.");

        this.feature_name = SkeletonFeature.FEATURE_NAME;
        SkeletonFeature.logger.debug("this.element_name = {}", this.feature_name);
    }

    static class SkeletonFeatureException extends Exception {
        String msg;
        public SkeletonFeatureException(String m) {
            this.msg = m;
        }
    }

    /**
     * This function creates a skeleton object from a string
     */
    static public SkeletonNode readSkeleton(String s) throws Exception {
        SkeletonFeature.logger.debug("Reading skeleton from string:");
        SkeletonFeature.logger.debug(s);
        SkeletonFeature.sexpr_scanner = new SExprScanner(s);
        return readSExpr();
    }

    static private List<SkeletonNode> readSExprList() throws Exception {
        ArrayList<SkeletonNode> children = new ArrayList<>(10);
        while(sexpr_scanner.peekChar() == '(') {
            children.add(readSExpr());
        }
        return children;
    }

    static private String readOp() throws Exception {
        return sexpr_scanner.nextOp();
    }

    /**
     * This is the helper function used to read skeleton s expressions
     */
    static private SkeletonNode readSExpr() throws Exception {
        // read '('
        assert((char) sexpr_scanner.nextChar() == '(');
        sexpr_scanner.skipSpace();

        // check for empty sexpr
        if((char) sexpr_scanner.peekChar() == ')') {
            assert((char) sexpr_scanner.nextChar() == ')');
            sexpr_scanner.skipSpace();
            return Empty.ONLY;
        }

        // read car
        SkeletonNode node;
        String atom = sexpr_scanner.nextWord();
        sexpr_scanner.skipSpace();
        switch(atom) {
            case "cond":
                node = new Cond(readSExprList());
                break;
            case "seq":
                node = new Seq(readSExprList());
                break;
            case "loop":
                node = new Loop(readSExprList());
                break;
            case "op":
                node = new Op(readOp());
                break;
            default:
                throw new SkeletonFeatureException("Unknown atom: " + atom);
        }

        // read ')'
        assert((char) sexpr_scanner.nextChar() == ')');
        sexpr_scanner.skipSpace();
        return node;
    }

    /**
     * A scanner for s expressions
     */
    static public class SExprScanner {
        private BufferedReader reader;
        public SExprScanner(String source) {
            this.reader = new BufferedReader(new StringReader(source));
            assert(this.reader.markSupported());
        }

        public void skipSpace() throws IOException {
            while(this.peekChar() != -1 && Character.isSpaceChar(this.peekChar())) {
                this.nextChar();
            }
        }

        public int nextChar() throws IOException {
            return this.reader.read();
        }

        public int peekChar() throws IOException {
            this.reader.mark(1);
            int c = this.reader.read();
            this.reader.reset();
            return c;
        }

        public String nextWord() throws IOException {
            StringBuilder builder = new StringBuilder();
            while(Character.isAlphabetic(this.peekChar())) {
                builder.append((char) this.nextChar());
            }
            return builder.toString();
        }

        public String nextOp() throws IOException {
            StringBuilder builder = new StringBuilder();
            char p = (char) this.peekChar();
            while(p == '+' || p == '-' || p == '*' || p == '/' ||
                  p == '>' || p == '<' || p == '=') {
                builder.append((char) this.nextChar());
            }
            return builder.toString();
        }
    }

    /**
     * This is the AST for skeleton. A skeleton node could be a condition node, a sequence node, a loop node or an
     * empty node.
     */
    public static abstract class SkeletonNode {
        static public final int COND = 0;
        static public final int SEQ = 1;
        static public final int LOOP = 2;
        static public final int EMPTY = 3;
        static public final int OP = 4;
        public int TYPE = -1;
    }
    public static class Cond extends SkeletonNode {
        public List<SkeletonNode> children;
        public Cond(List<SkeletonNode> c) {this.children = c; this.TYPE = SkeletonNode.COND;}

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof Cond)) {
                return false;
            }
            Cond other_cond = (Cond) o;
            return this.children.equals(other_cond.children);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("(cond ");
            if(this.children.isEmpty()) {
                result.append("()");
            } else {
                for(SkeletonNode c : this.children) {
                    result.append(c.toString());
                }
            }
            result.append(")");
            return result.toString();
        }
    }
    public static class Seq extends SkeletonNode {
        public List<SkeletonNode> children;
        public Seq(List<SkeletonNode> c) {this.children = c; this.TYPE = SkeletonNode.SEQ;}

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof Seq)) {
                return false;
            }
            Seq other_seq = (Seq) o;
            return this.children.equals(other_seq.children);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("(seq ");
            for(SkeletonNode c : this.children) {
                result.append(c.toString());
            }
            result.append(")");
            return result.toString();
        }
    }
    public static class Loop extends SkeletonNode {
        public List<SkeletonNode> children;
        public Loop(List<SkeletonNode> c) {this.children = c; this.TYPE = SkeletonNode.LOOP;}

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof Loop)) {
                return false;
            }
            Loop other_loop = (Loop) o;
            return this.children.equals(other_loop.children);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("(loop ");
            for(SkeletonNode c : this.children) {
                result.append(c.toString());
            }
            result.append(")");
            return result.toString();
        }
    }

    public static class Op extends SkeletonNode {
        public String operation;
        public Op(String o) {this.operation = o; this.TYPE = SkeletonNode.OP;}

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof Op)) {
                return false;
            }
            Op other_op = (Op) o;
            return this.operation.equals(other_op.operation);
        }

        @Override
        public String toString() {
            return this.operation;
        }
    }
    public static class Empty extends SkeletonNode {
        static public Empty ONLY = new Empty();
        private Empty() {this.TYPE = SkeletonNode.EMPTY;}

        @Override
        public boolean equals(Object o) {
            return o instanceof Empty;
        }

        @Override
        public String toString() { return "()"; }
    }

}
