package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.List;

public interface Metric {
    float compute(DSubTree originalAST, List<DSubTree> predictedASTs);
}
