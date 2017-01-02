package dsl;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NumberLiteral;

public class RefinementNumber extends Refinement {
    boolean exists;
    float value;

    public RefinementNumber(Expression e, Visitor visitor) {
        super(visitor);
        if (knownConstants(e))
            return;

        if (! (e instanceof NumberLiteral)) {
            this.exists = false;
            this.value = 0;
            return;
        }

        this.exists = true;
        this.value = Float.parseFloat(((NumberLiteral) e).getToken());
    }

    private boolean knownConstants(Expression e) {
        if (! (e instanceof Name))
            return false;
        String s = ((Name) e).getFullyQualifiedName();
        if (visitor.options.KNOWN_CONSTANTS_NUMBER.containsKey(s)) {
            this.exists = true;
            this.value = visitor.options.KNOWN_CONSTANTS_NUMBER.get(s);
            return true;
        }
        return false;
    }
}
