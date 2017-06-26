package edu.rice.cs.caper.bayou.application.experiments.predit_asts;

import edu.rice.cs.caper.bayou.core.dsl.DAPICall;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JaccardAPICallsMetric extends MetricCalculator {

    public JaccardAPICallsMetric(DSubTree originalAST, List<DSubTree> predictedASTs) {
        super(originalAST, predictedASTs, null, null);
    }

    @Override
    public void doCalculation() {
        float jaccard = 1;
        Set<DAPICall> A = originalAST.bagOfAPICalls();
        for (DSubTree predictedAST : predictedASTs) {
            Set<DAPICall> B = predictedAST.bagOfAPICalls();

            // A union B
            Set<DAPICall> AunionB = new HashSet<>();
            AunionB.addAll(A);
            AunionB.addAll(B);

            // A intersect B
            Set<DAPICall> AinterB = new HashSet<>();
            AinterB.addAll(A);
            AinterB.retainAll(B);

            float j = 1 - ((float) AinterB.size()) / AunionB.size();
            if (j < jaccard)
                jaccard = j;
        }
        System.out.println(String.format("Min Jaccard distance: %3.2f", jaccard));
    }
}
