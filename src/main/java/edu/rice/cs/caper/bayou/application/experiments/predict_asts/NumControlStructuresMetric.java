package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NumControlStructuresMetric implements Metric {

    /** Computes the minimum ratio of the difference between the number of
     * control structures in the original vs predicted ASTs.
     */
    @Override
    public float compute(DSubTree originalAST, List<DSubTree> predictedASTs) {
        int original = 1 /* the body itself */+ originalAST.numBranches() + originalAST.numLoops() + originalAST.numExcepts();
        List<Integer> diffs = new ArrayList<>();
        diffs.add(original);
        for (DSubTree predictedAST : predictedASTs) {
            int predicted = 1 + predictedAST.numBranches() + predictedAST.numLoops() + predictedAST.numExcepts();
            int diff_predicted = Math.abs(predicted - original);
            diffs.add(diff_predicted);
        }
        float min_diff = Collections.min(diffs);
        return min_diff / original;
    }
}
