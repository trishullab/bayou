package dsl;

import org.eclipse.jdt.core.dom.Expression;

import java.util.ArrayList;
import java.util.List;

public class Refinement {

    protected transient Visitor visitor;
    public Refinement(Visitor visitor) {
        this.visitor = visitor;
    }

    public static List<Refinement> getRefinements(Expression e, Visitor visitor) {
        List<Refinement> refinements = new ArrayList<>();

        /* Add here any new refinement types. If the refinement requires a new kind
         * of return type apart from these, then add it to the DSL and make it
         * extend this class (Refinement). */
        refinements.add(new RefinementBoolean(e, visitor));
        refinements.add(new RefinementNumber(e, visitor));
        refinements.add(new RefinementString(e, visitor));

        return refinements;
    }
}
