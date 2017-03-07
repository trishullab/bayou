package edu.rice.bayou.dom_driver;

import org.eclipse.jdt.core.dom.Expression;

import java.util.ArrayList;
import java.util.List;

public class Refinement {

    public static List<Refinement> getRefinements(Expression e) {
        List<Refinement> refinements = new ArrayList<>();

        /* Add here any new refinement types. If the refinement requires a new kind
         * of return type apart from these, then add it to the DSL and make it
         * extend this class (Refinement). */
        refinements.add(new RefinementBoolean(e));
        refinements.add(new RefinementNumber(e));
        refinements.add(new RefinementString(e));

        return refinements;
    }
}
