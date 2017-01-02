package dsl;

import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;

public class RefinementBoolean extends Refinement {
    boolean exists;
    boolean value;

    public RefinementBoolean(Expression e, Visitor visitor) {
        super(visitor);
        if (knownConstants(e))
            return;

        if (! (e instanceof BooleanLiteral)) {
            this.exists = false;
            this.value = false;
            return;
        }

        this.exists = true;
        this.value = ((BooleanLiteral) e).booleanValue();
    }

    private boolean knownConstants(Expression e) {
        if (! (e instanceof Name))
            return false;
        String s = ((Name) e).getFullyQualifiedName();
        if (visitor.options.KNOWN_CONSTANTS_BOOLEAN.containsKey(s)) {
            this.exists = true;
            this.value = visitor.options.KNOWN_CONSTANTS_BOOLEAN.get(s);
            return true;
        }
        return false;
    }
}
