package edu.rice.cs.caper.bayou.application.experiments.low_level_sketches;

import java.util.List;
import java.util.stream.Collectors;

public class DLoopLowLevel extends DASTNodeLowLevel {

    String node = "DLoop";
    List<DAPICallLowLevel> _cond;
    List<DASTNodeLowLevel> _body;

    @Override
    public String getLowLevelSketch() {
        return node + delim
                + String.join(delim, _cond.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP + delim
                + String.join(delim, _body.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP;
    }
}
