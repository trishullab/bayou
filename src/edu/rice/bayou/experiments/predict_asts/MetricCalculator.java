package edu.rice.bayou.experiments.predict_asts;

import edu.rice.bayou.dsl.DASTNode;
import edu.rice.bayou.dsl.DSubTree;
import edu.rice.bayou.dsl.Sequence;

import java.util.ArrayList;
import java.util.List;

public abstract class MetricCalculator {
    final DSubTree originalAST;
    final List<DSubTree> predictedASTs;
    final List<Sequence> givenSequences;
    final List<Sequence> unseenSequences;

    public abstract void doCalculation();

    protected List<Sequence> getGeneratedSequences(DSubTree predictedAST, int max, boolean withSubSeqs)
            throws DASTNode.TooManySequencesException {
        List<Sequence> generatedSeqs = new ArrayList<>();
        generatedSeqs.add(new Sequence());
        predictedAST.updateSequences(generatedSeqs, max);
        if (withSubSeqs) {
            int num = generatedSeqs.size();
            for (int i = 0; i < num; i++) {
                Sequence seq = generatedSeqs.get(i);
                for (int j = 1; j <= seq.getCalls().size(); j++) {
                    Sequence subseq = new Sequence(seq.getCalls().subList(0, j));
                    if (! generatedSeqs.contains(subseq))
                        generatedSeqs.add(subseq);
                }
            }
        }
        return generatedSeqs;
    }

    protected MetricCalculator(DSubTree originalAST, List<DSubTree> predictedASTs, List<Sequence> givenSequences,
                            List<Sequence> unseenSequences) {
        this.originalAST = originalAST;
        this.predictedASTs = predictedASTs;
        this.givenSequences = givenSequences;
        this.unseenSequences = unseenSequences;
    }
}
