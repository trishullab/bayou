package edu.rice.bayou.experiments.low_level_sketches;

import java.util.List;
import java.util.stream.Collectors;

public class DExceptLowLevel extends DASTNodeLowLevel {

    String node = "DExcept";
    public List<DASTNodeLowLevel> _try;
    public List<DASTNodeLowLevel> _catch;

    @Override
    public String getLowLevelSketch() {
        return node + delim
                + String.join(delim, _try.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP + delim
                + String.join(delim, _catch.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP;
    }
}
