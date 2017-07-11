package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import java.util.List;

public class EqualityASTMetric implements Metric {

    /** Computes whether the original AST is present exactly as it is in one
     * of the predicted ASTS.
     */
    @Override
    public float compute(DSubTree originalAST, List<DSubTree> predictedASTs, String aggregate) {
        boolean equals = false;
        for (DSubTree predictedAST : predictedASTs) {
            if (originalAST.equals(predictedAST)) {
                equals = true;
                break;
            }
        }
        return equals? 1 : 0;
    }
}
