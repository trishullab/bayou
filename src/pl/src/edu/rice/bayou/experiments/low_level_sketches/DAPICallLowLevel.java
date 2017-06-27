package edu.rice.bayou.experiments.low_level_sketches;

public class DAPICallLowLevel extends DASTNodeLowLevel {

    String node = "DAPICall";
    String _call;

    @Override
    public String getLowLevelSketch() {
        String[] tokens = _call.split("[,()]");
        return node + delim + String.join(delim, tokens) + delim + STOP;
    }
}
