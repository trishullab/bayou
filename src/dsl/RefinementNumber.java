package dsl;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.NumberLiteral;

public class RefinementNumber extends Refinement {
    boolean exists;
    float value;

    public RefinementNumber(Expression e) {
        if (! (e instanceof NumberLiteral)) {
            this.exists = false;
            this.value = 0;
            return;
        }

        this.exists = true;
        this.value = Float.parseFloat(((NumberLiteral) e).getToken());
    }
}
