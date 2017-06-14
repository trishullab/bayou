package edu.rice.cs.caper.lib.bayou.synthesizer;

import edu.rice.cs.caper.lib.bayou.dsl.BindingNotFoundException;
import org.eclipse.jdt.core.dom.ASTNode;

public interface Synthesizable {
    ASTNode synthesize(Environment env) throws ClassNotFoundException, BindingNotFoundException;
}
