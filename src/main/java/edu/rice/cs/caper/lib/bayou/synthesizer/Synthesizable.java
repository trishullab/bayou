package edu.rice.cs.caper.lib.bayou.synthesizer;

import org.eclipse.jdt.core.dom.ASTNode;

public interface Synthesizable {
    ASTNode synthesize(Environment env);
}
