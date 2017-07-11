package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NumStatementsMetric implements Metric {

    /** Computes the minimum ratio of the difference between the number of
     * statements in the original vs predicted ASTs.
     */
    @Override
    public float compute(DSubTree originalAST, List<DSubTree> predictedASTs, String aggregate) {
        int original = originalAST.numStatements();
        List<Integer> diffs = new ArrayList<>();
        diffs.add(original);
        for (DSubTree predictedAST : predictedASTs) {
            int predicted = predictedAST.numStatements();
            int diff_predicted = Math.abs(predicted - original);
            diffs.add(diff_predicted);
        }
        float aggr_diff = Metric.aggregate(diffs, aggregate);
        return aggr_diff / original;
    }
}
