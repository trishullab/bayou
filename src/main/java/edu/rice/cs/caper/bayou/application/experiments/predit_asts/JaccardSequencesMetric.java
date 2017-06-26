package edu.rice.cs.caper.bayou.application.experiments.predit_asts;


import edu.rice.cs.caper.bayou.core.dsl.DASTNode;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JaccardSequencesMetric extends MetricCalculator {

    public JaccardSequencesMetric(List<DSubTree> predictedASTs, List<Sequence> givenSequences, List<Sequence> unseenSequences) {
        super(null, predictedASTs, givenSequences, unseenSequences);
    }

    @Override
    public void doCalculation() {
        List<Sequence> A = new ArrayList<>(givenSequences);
        A.addAll(unseenSequences);

        float jaccard = 1;
        for (DSubTree predictedAST : predictedASTs) {
            List<Sequence> B;
            try {
                B = getGeneratedSequences(predictedAST, 10, 10, true);
            } catch (DASTNode.TooManySequencesException|DASTNode.TooLongSequenceException e) {
                continue;
            }

            // A union B
            Set<Sequence> AunionB = new HashSet<>();
            AunionB.addAll(A);
            AunionB.addAll(B);

            // A intersect B
            Set<Sequence> AinterB = new HashSet<>();
            AinterB.addAll(A);
            AinterB.retainAll(B);

            float j = 1 - ((float) AinterB.size()) / AunionB.size();
            if (j < jaccard)
                jaccard = j;
        }
        System.out.println(String.format("Min Jaccard distance: %3.2f", jaccard));
    }
}
