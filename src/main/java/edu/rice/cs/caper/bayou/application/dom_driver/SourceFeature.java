package edu.rice.cs.caper.bayou.application.dom_driver;

/**
 * This is the parent class for all source-level features
 */
abstract public class SourceFeature {
    public String feature_name;


    @Override
    public boolean equals(Object other) {
        if(!(other instanceof SourceFeature)) return false;
        SourceFeature f = (SourceFeature) other;

        return f.feature_name.equals(this.feature_name);
    }
}
