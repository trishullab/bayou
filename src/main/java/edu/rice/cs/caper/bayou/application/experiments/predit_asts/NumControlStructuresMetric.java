package edu.rice.cs.caper.bayou.application.experiments.predit_asts;


import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.List;

public class NumControlStructuresMetric extends MetricCalculator {

    /* roughly corresponds to number of lines of code */
    public NumControlStructuresMetric(DSubTree originalAST, List<DSubTree> predictedASTs) {
        super(originalAST, predictedASTs, null, null);
    }

    @Override
    public void doCalculation() {
        int original = 1 /* the body itself */+ originalAST.numBranches() + originalAST.numLoops() + originalAST.numExcepts();
        int diff = original;
        for (DSubTree predictedAST : predictedASTs) {
            int predicted = 1 + predictedAST.numBranches() + predictedAST.numLoops() + predictedAST.numExcepts();
            int diff_predicted = predicted > original? predicted - original: original - predicted;
            if (diff_predicted < diff)
                diff = diff_predicted;
        }
        float ratio_diff = ((float) diff)/original;
        System.out.println("Min ratio difference in number of control structures: " + ratio_diff);
    }
}

