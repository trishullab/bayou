package dsl;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.StringLiteral;

import java.util.regex.Pattern;

public class RefinementString extends Refinement {
    boolean exists;
    int length;
    boolean containsPunct;

    public RefinementString(Expression e) {
        if ( !(e instanceof StringLiteral)) {
            this.exists = false;
            this.length = 0;
            this.containsPunct = false;
            return;
        }

        StringLiteral s = (StringLiteral) e;
        Pattern p = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE);
        this.exists = true;
        this.length = s.getLiteralValue().length();
        this.containsPunct = p.matcher(s.getLiteralValue()).find();
    }
}
