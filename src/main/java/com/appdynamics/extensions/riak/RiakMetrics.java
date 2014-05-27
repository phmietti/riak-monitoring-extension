package com.appdynamics.extensions.riak;


import java.util.HashMap;
import java.util.Map;

public class RiakMetrics {

    private String displayName;
    private Map<String,String> metrics;

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Map<String, String> getMetrics() {
        if(metrics == null){
            metrics = new HashMap<String, String>();
        }
        return metrics;
    }

    public void setMetrics(Map<String, String> metrics) {
        this.metrics = metrics;
    }


}
