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
public class DecoratedSkeletonFeature extends SourceFeature {
    private static final String FEATURE_NAME = "Decorated Skeleton";

    private SkeletonNode skeleton;
    private static final Logger logger = LogManager.getLogger(DecoratedSkeletonFeature.class.getName());

    // this is used to extract the skeleton
    static private SExprScanner sexpr_scanner;

    @Override
    public boolean equals(Object other) {
        return other instanceof DecoratedSkeletonFeature &&
                ((DecoratedSkeletonFeature) other).skeleton.equals(this.skeleton) &&
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
                return ((Seq) n).children.stream().anyMatch(DecoratedSkeletonFeature::is_interesting);
            default: return false;
        }
    }

    /**
     * Get a list of skeleton node from a list of statements
     */
    static private Seq gen_seq(List<Statement> slist) {
        List<SkeletonNode> children = new ArrayList<>();
        for (Statement stmt : slist) {
            switch (stmt.getNodeType()) {
                case ASTNode.DO_STATEMENT:
                case ASTNode.ENHANCED_FOR_STATEMENT:
                case ASTNode.FOR_STATEMENT:
                case ASTNode.IF_STATEMENT:
                case ASTNode.LABELED_STATEMENT:
                case ASTNode.SWITCH_STATEMENT:
                case ASTNode.SYNCHRONIZED_STATEMENT:
                case ASTNode.TRY_STATEMENT:
                case ASTNode.WHILE_STATEMENT:
                    children.add(gen_skeleton(stmt));
                    break;
                default:
                    children.addAll(DecoratedSkeletonFeature.get_op(stmt));
                    break;
            }
        }

        return new Seq(children.stream().filter(DecoratedSkeletonFeature::is_interesting).collect(Collectors.toList()));
    }

    /**
     * This is the main function for generating a skeleton node given a java statement. It only consider certain
     * statements. It doesn't consider statements such as synchronization or exceptions.
     */
    static private SkeletonNode gen_skeleton(Statement s) {
        DecoratedSkeletonFeature.logger.debug("Looking at statement '{}'", s.toString());
        if(s.getNodeType() == ASTNode.BLOCK) {
            DecoratedSkeletonFeature.logger.debug("This is a block.");
            Block block = (Block) s;
            Seq node = DecoratedSkeletonFeature.gen_seq(block.statements());
            DecoratedSkeletonFeature.logger.debug("node = {}", node.toString());
            if(node.children.isEmpty()) {
                DecoratedSkeletonFeature.logger.debug("Empty sequence.");
                return Empty.ONLY;
            } else {
                DecoratedSkeletonFeature.logger.debug("Non-empty sequence.");
                return node;
            }
        } else if(s.getNodeType() == ASTNode.DO_STATEMENT) {
            DecoratedSkeletonFeature.logger.debug("This is a do statement.");
            DoStatement dstmt = (DoStatement) s;
            List<Statement> body = new ArrayList<>();

            // check the body
            Expression test_expr = (Expression) ASTNode.copySubtree(dstmt.getAST(), dstmt.getExpression());
            body.add(dstmt.getAST().newExpressionStatement(test_expr));
            if(dstmt.getBody().getNodeType() == ASTNode.BLOCK) {
                body.addAll(((Block) dstmt.getBody()).statements());
            } else {
                body.add(dstmt.getBody());
            }

            Seq node = DecoratedSkeletonFeature.gen_seq(body);
            DecoratedSkeletonFeature.logger.debug("node = {}", node.toString());

            return new Loop(node);
        } else if(s.getNodeType() == ASTNode.ENHANCED_FOR_STATEMENT) {
            DecoratedSkeletonFeature.logger.debug("This is an enhanced for statement.");
            SkeletonNode child = DecoratedSkeletonFeature.gen_skeleton(((EnhancedForStatement) s).getBody());
            DecoratedSkeletonFeature.logger.debug("child = {}", child.toString());

            if(DecoratedSkeletonFeature.is_interesting(child)) {
                DecoratedSkeletonFeature.logger.debug("Interesting child.");
                return new Loop(child);
            } else {
                DecoratedSkeletonFeature.logger.debug("No interesting child.");
                return new Loop(Empty.ONLY);
            }

        } else if(s.getNodeType() == ASTNode.FOR_STATEMENT) {
            DecoratedSkeletonFeature.logger.debug("This is a for statement.");
            ForStatement fstmt = (ForStatement) s;
            List<Statement> body = new ArrayList<>();

            // check the inits and updates
            for(int i = 0; i < fstmt.initializers().size(); ++i) {
                Expression init = (Expression) ASTNode.copySubtree(fstmt.getAST(), (ASTNode) fstmt.initializers().get(i));
                body.add(fstmt.getAST().newExpressionStatement(init));
            }
            if(fstmt.getBody().getNodeType() == ASTNode.BLOCK) {
                body.addAll(((Block) fstmt.getBody()).statements());
            } else {
                body.add(fstmt.getBody());
            }
            for(int i = 0; i < fstmt.updaters().size(); ++i) {
                Expression update = (Expression) ASTNode.copySubtree(fstmt.getAST(), (ASTNode) fstmt.updaters().get(i));
                body.add(fstmt.getAST().newExpressionStatement(update));
            }

            Seq node = DecoratedSkeletonFeature.gen_seq(body);
            DecoratedSkeletonFeature.logger.debug("node = {}", body.toString());

            return new Loop(node);
        } else if(s.getNodeType() == ASTNode.IF_STATEMENT) {
            DecoratedSkeletonFeature.logger.debug("This is an if statement.");
            IfStatement ifs = (IfStatement) s;
            List<SkeletonNode> children = new ArrayList<>();

            List<Statement> body = new ArrayList<>();

            // check the body
            {
                if(ifs.getThenStatement().getNodeType() == ASTNode.BLOCK) {
                    body.addAll(((Block) ifs.getThenStatement()).statements());
                } else {
                    body.add(ifs.getThenStatement());
                }

            }

            // check the else branch
            if(ifs.getElseStatement() != null) {
                if(ifs.getElseStatement().getNodeType() == ASTNode.BLOCK) {
                    body.addAll(((Block) ifs.getElseStatement()).statements());
                } else {
                    body.add(ifs.getElseStatement());
                }

            }

            // check the test expression
            {
                Expression test_expr = (Expression) ASTNode.copySubtree(ifs.getAST(), ifs.getExpression());
                body.add(ifs.getAST().newExpressionStatement(test_expr));
            }

            Seq node = DecoratedSkeletonFeature.gen_seq(body);

            // if we have nothing, add an empty node
            DecoratedSkeletonFeature.logger.debug("We have {} children.", children.size());
            if(node.children.isEmpty()) {
                children.add(Empty.ONLY);
            } else {
                children.add(node);
            }

            return new Cond(children);
        } else if(s.getNodeType() == ASTNode.LABELED_STATEMENT) {
            DecoratedSkeletonFeature.logger.debug("This is a labeled statement.");
            return DecoratedSkeletonFeature.gen_skeleton(((LabeledStatement) s).getBody());
        } else if(s.getNodeType() == ASTNode.SWITCH_STATEMENT) {
            DecoratedSkeletonFeature.logger.warn("Ignoring switch statement.");
            return Empty.ONLY;
        } else if(s.getNodeType() == ASTNode.SYNCHRONIZED_STATEMENT) {
            DecoratedSkeletonFeature.logger.warn("Ignoring synchronized statement.");
            return DecoratedSkeletonFeature.gen_skeleton(((SynchronizedStatement) s).getBody());
        } else if(s.getNodeType() == ASTNode.TRY_STATEMENT) {
            DecoratedSkeletonFeature.logger.warn("Ignoring try statement.");
            List<Statement> stmts = new ArrayList<>();
            TryStatement ts = (TryStatement) s;
            stmts.addAll(ts.getBody().statements());
            if(ts.getFinally() != null) {
                stmts.addAll(ts.getFinally().statements());

            }
            return DecoratedSkeletonFeature.gen_seq(stmts);
        } else if(s.getNodeType() == ASTNode.WHILE_STATEMENT) {
            DecoratedSkeletonFeature.logger.debug("This is a while statement.");

            // get all children together
            WhileStatement wstmt = (WhileStatement) s;
            List<Statement> body = new ArrayList<>();

            // check the body
            Expression test_expr = (Expression) ASTNode.copySubtree(wstmt.getAST(), wstmt.getExpression());
            body.add(wstmt.getAST().newExpressionStatement(test_expr));
            if(wstmt.getBody().getNodeType() == ASTNode.BLOCK) {
                body.addAll(((Block) wstmt.getBody()).statements());
            } else {
                body.add(wstmt.getBody());
            }

            Seq node = DecoratedSkeletonFeature.gen_seq(body);
            DecoratedSkeletonFeature.logger.debug("node = {}", node.toString());

            return new Loop(node);
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

    public DecoratedSkeletonFeature(MethodDeclaration input) {
        DecoratedSkeletonFeature.logger.debug("Generating skeleton feature...");
        Statement s = input.getBody();
        if(s == null) {
            this.skeleton = Empty.ONLY;
        } else {
            this.skeleton = DecoratedSkeletonFeature.gen_skeleton(s);
        }
        DecoratedSkeletonFeature.logger.debug("Done generating skeleton feature.");

        this.feature_name = DecoratedSkeletonFeature.FEATURE_NAME;
        DecoratedSkeletonFeature.logger.debug("this.element_name = {}", this.feature_name);
    }

    static class SkeletonFeatureException extends Exception {
        String msg;
        SkeletonFeatureException(String m) {
            this.msg = m;
        }
    }

    /**
     * This function creates a skeleton object from a string
     */
    static public SkeletonNode readSkeleton(String s) throws Exception {
        DecoratedSkeletonFeature.logger.debug("Reading skeleton from string:");
        DecoratedSkeletonFeature.logger.debug(s);
        DecoratedSkeletonFeature.sexpr_scanner = new SExprScanner(s);
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
                node = new Loop(readSExpr());
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
        SExprScanner(String source) {
            this.reader = new BufferedReader(new StringReader(source));
            assert(this.reader.markSupported());
        }

        void skipSpace() throws IOException {
            while(this.peekChar() != -1 && Character.isSpaceChar(this.peekChar())) {
                this.nextChar();
            }
        }

        int nextChar() throws IOException {
            return this.reader.read();
        }

        int peekChar() throws IOException {
            this.reader.mark(1);
            int c = this.reader.read();
            this.reader.reset();
            return c;
        }

        String nextWord() throws IOException {
            StringBuilder builder = new StringBuilder();
            while(Character.isAlphabetic(this.peekChar())) {
                builder.append((char) this.nextChar());
            }
            return builder.toString();
        }

        String nextOp() throws IOException {
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
    static abstract class SkeletonNode {
        static final int COND = 0;
        static final int SEQ = 1;
        static final int LOOP = 2;
        static final int EMPTY = 3;
        static final int OP = 4;
        int TYPE = -1;
    }
    public static class Cond extends SkeletonNode {
        List<SkeletonNode> children;
        Cond(List<SkeletonNode> c) {this.children = c; this.TYPE = SkeletonNode.COND;}

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
                for(int i = 0; i < this.children.size(); ++i) {
                    SkeletonNode c = this.children.get(i);
                    if(i > 0) {
                        result.append(" ");
                    }
                    result.append(c.toString());
                }
            }
            result.append(")");
            return result.toString();
        }
    }
    public static class Seq extends SkeletonNode {
        List<SkeletonNode> children;
        Seq(List<SkeletonNode> c) {this.children = c; this.TYPE = SkeletonNode.SEQ;}

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
            for(int i = 0; i < this.children.size(); ++i) {
                SkeletonNode c = this.children.get(i);
                if(i > 0) {
                    result.append(" ");
                }
                result.append(c.toString());
            }
            result.append(")");
            return result.toString();
        }
    }
    public static class Loop extends SkeletonNode {
        SkeletonNode child;
        Loop(SkeletonNode c) {this.child = c; this.TYPE = SkeletonNode.LOOP;}

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof Loop)) {
                return false;
            }
            Loop other_loop = (Loop) o;
            return this.child.equals(other_loop.child);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("(loop ");
            result.append(this.child.toString());
            result.append(")");
            return result.toString();
        }
    }

    public static class Op extends SkeletonNode {
        String operation;
        Op(String o) {this.operation = o; this.TYPE = SkeletonNode.OP;}

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
        static Empty ONLY = new Empty();
        private Empty() {this.TYPE = SkeletonNode.EMPTY;}

        @Override
        public boolean equals(Object o) {
            return o instanceof Empty;
        }

        @Override
        public String toString() { return "()"; }
    }

}
