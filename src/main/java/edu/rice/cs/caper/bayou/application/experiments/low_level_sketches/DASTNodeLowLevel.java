package edu.rice.cs.caper.bayou.application.experiments.low_level_sketches;

public abstract class DASTNodeLowLevel {
    transient String delim = " ";
    transient String STOP = "STOP";
    abstract String getLowLevelSketch();
}
