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

import edu.rice.cs.caper.bayou.core.dsl.DAPICall;
import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.*;

public class JaccardAPICallsMetric implements Metric {

    /** Computes the minimum Jaccard distance on the set of API calls
     * between the original and the predicted ASTs.
     */
    @Override
    public float compute(DSubTree originalAST, List<DSubTree> predictedASTs, String aggregate) {
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
        return Metric.aggregate(jaccard, aggregate);
    }
}
