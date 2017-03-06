package edu.rice.bayou.experiments.predict_asts;

import edu.rice.bayou.dsl.DSubTree;

import java.util.List;

public class NumStatementsMetric extends MetricCalculator {

    /* roughly corresponds to number of lines of code */
    public NumStatementsMetric(DSubTree originalAST, List<DSubTree> predictedASTs) {
        super(originalAST, predictedASTs, null, null);
    }

    @Override
    public void doCalculation() {
        int original = originalAST.numStatements();
        int diff = original;
        for (DSubTree predictedAST : predictedASTs) {
            int predicted = predictedAST.numStatements();
            int diff_predicted = predicted > original? predicted - original: original - predicted;
            if (diff_predicted < diff)
                diff = diff_predicted;
        }
        float ratio_diff = ((float) diff)/original;
        System.out.println("Min ratio difference in number of statements: " + ratio_diff);
    }
}
