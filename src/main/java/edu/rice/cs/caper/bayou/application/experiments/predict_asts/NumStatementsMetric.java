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
