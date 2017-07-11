package edu.rice.cs.caper.bayou.application.experiments.predict_asts;

import edu.rice.cs.caper.bayou.core.dsl.DSubTree;

import java.util.Collections;
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
        float min = Float.MAX_VALUE;
        for (Number v: values)
            if (v.floatValue() < min)
                min = v.floatValue();
        return min;
    }

    static float mean(List<? extends Number> values) {
        float average = 0;
        for (Number v: values)
            average += v.floatValue();
        average /= values.size();
        return average;
    }

    static float standardDeviation(List<? extends Number> values) {
        float average = mean(values);
        float stdv = 0;
        for (Number v: values)
            stdv += Math.pow(v.floatValue() - average, 2);
        stdv /= values.size();
        return (float) Math.sqrt(stdv);
    }
}
