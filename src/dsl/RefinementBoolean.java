package dsl;

import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;

public class RefinementBoolean extends Refinement {
    boolean exists;
    boolean value;

    public RefinementBoolean(Expression e) {
        if (! (e instanceof BooleanLiteral)) {
            this.exists = false;
            this.value = false;
            return;
        }

        this.exists = true;
        this.value = ((BooleanLiteral) e).booleanValue();
    }
}
