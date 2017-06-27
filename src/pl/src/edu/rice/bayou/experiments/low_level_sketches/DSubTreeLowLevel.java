package edu.rice.bayou.experiments.low_level_sketches;

import java.util.List;
import java.util.stream.Collectors;

public class DSubTreeLowLevel extends DASTNodeLowLevel {

    String node = "DSubTree";
    List<DASTNodeLowLevel> _nodes;

    @Override
    public String getLowLevelSketch() {
        return node + delim
                + String.join(delim, _nodes.stream().map(d -> d.getLowLevelSketch()).collect(Collectors.toList()))
                + delim + STOP;
    }
}
