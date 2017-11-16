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
package edu.rice.cs.caper.bayou.core.sketch_metric;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import java.util.List;

public class EqualityASTMetric implements Metric {

    /** Computes whether the original AST is present exactly as it is in one
     * of the predicted ASTS.
     */
    @Override
    public float compute(DSubTree originalAST, List<DSubTree> predictedASTs, String aggregate) {
        boolean equals = false;
        for (DSubTree predictedAST : predictedASTs) {
            if (originalAST.equals(predictedAST)) {
                equals = true;
                break;
            }
        }
        return equals? 1 : 0;
    }
}
