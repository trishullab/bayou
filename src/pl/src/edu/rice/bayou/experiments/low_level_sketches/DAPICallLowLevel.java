package edu.rice.bayou.experiments.low_level_sketches;

public class DAPICallLowLevel extends DASTNodeLowLevel {

    String node = "DAPICall";
    String _call;

    @Override
    public String getLowLevelSketch() {
        return node + delim + _call + delim + STOP;
    }
}
