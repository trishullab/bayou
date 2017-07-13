package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;
import org.apache.commons.math3.stat.StatUtils;

import java.util.List;

public interface Metric {
    float compute(DSubTree originalAST, List<DSubTree> predictedASTs, String aggregate);

    static float aggregate(List<? extends Number> values, String aggregate) {
        switch (aggregate) {
            case "min":
                return min(values);
            case "mean":
                return mean(values);
            case "stdv":
                return standardDeviation(values);
            default:
                throw new Error("Invalid aggregate measure: " + aggregate);
        }
    }

    static float min(List<? extends Number> values) {
        double[] dValues = values.stream().mapToDouble(v -> v.floatValue()).toArray();
        return (float) StatUtils.min(dValues);
    }

    static float mean(List<? extends Number> values) {
        double[] dValues = values.stream().mapToDouble(v -> v.floatValue()).toArray();
        return (float) StatUtils.mean(dValues);
    }

    static float standardDeviation(List<? extends Number> values) {
        double[] dValues = values.stream().mapToDouble(v -> v.floatValue()).toArray();
        return (float) Math.sqrt(StatUtils.variance(dValues));
    }
}
