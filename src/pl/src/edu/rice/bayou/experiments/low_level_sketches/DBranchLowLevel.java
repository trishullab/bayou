package edu.rice.bayou.experiments.low_level_sketches;

import java.util.List;
import java.util.stream.Collectors;

public class DBranchLowLevel extends DASTNodeLowLevel {

    String node = "DBranch";
    List<DAPICallLowLevel> _cond;
    List<DASTNodeLowLevel> _then;
    List<DASTNodeLowLevel> _else;

    @Override
    public String getLowLevelSketch() {
        return node + delim
                + String.join(delim, _cond.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP + delim
                + String.join(delim, _then.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP + delim
                + String.join(delim, _else.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP;
    }
}
