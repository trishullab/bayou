package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DAPICall;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.*;

public class JaccardAPICallsMetric implements Metric {

    /** Computes the minimum Jaccard distance on the set of API calls
     * between the original and the predicted ASTs.
     */
    @Override
    public float compute(DSubTree originalAST, List<DSubTree> predictedASTs) {
        List<Float> jaccard = new ArrayList<>();
        jaccard.add((float) 1);
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

            jaccard.add(1 - ((float) AinterB.size()) / AunionB.size());
        }
        return Collections.min(jaccard);
    }
}
