/*
Copyright 2017 Rice University

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DASTNode;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import edu.rice.cs.caper.bayou.core.dsl.Sequence;

import java.util.*;

public class JaccardSequencesMetric implements Metric {

    /** Computes the minimum Jaccard distance on the set of sequences of API calls
     * between the original and the predicted ASTs.
     */
    @Override
    public float compute(DSubTree originalAST, List<DSubTree> predictedASTs) {
        List<Float> jaccard = new ArrayList<>();
        jaccard.add((float) 1);
        Set<Sequence> A;
        try {
            List<Sequence> _A = new ArrayList<>();
            _A.add(new Sequence());
            originalAST.updateSequences(_A, 999, 999);
            A = new HashSet<>(_A);
        } catch (DASTNode.TooManySequencesException|DASTNode.TooLongSequenceException e) {
            return (float) 1;
        }
        for (DSubTree predictedAST : predictedASTs) {
            Set<Sequence> B;
            try {
                List<Sequence> _B = new ArrayList<>();
                _B.add(new Sequence());
                predictedAST.updateSequences(_B, 999, 999);
                B = new HashSet<>(_B);
            } catch (DASTNode.TooManySequencesException|DASTNode.TooLongSequenceException e) {
                jaccard.add((float) 1);
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

            jaccard.add(1 - ((float) AinterB.size()) / AunionB.size());
        }
        return Collections.min(jaccard);
    }
}
