package com.appdynamics.extensions.riak.config;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Configuration {

    private Server[] servers;
    private String metricPrefix;
    private int threadTimeout;
    private int numberOfThreads;
    private Map<String,String> httpConfig = new HashMap<String,String>();
    private Set<String> metrics = new HashSet<String>();

    public Server[] getServers() {
        return servers;
    }

    public void setServers(Server[] servers) {
        this.servers = servers;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    public int getThreadTimeout() {
        return threadTimeout;
    }

    public void setThreadTimeout(int threadTimeout) {
        this.threadTimeout = threadTimeout;
    }

    public int getNumberOfThreads() {
        return numberOfThreads;
    }

    public void setNumberOfThreads(int numberOfThreads) {
        this.numberOfThreads = numberOfThreads;
    }

    public Set<String> getMetrics() {
        return metrics;
    }

    public void setMetrics(Set<String> metrics) {
        this.metrics = metrics;
    }

    public Map<String, String> getHttpConfig() {
        return httpConfig;
    }

    public void setHttpConfig(Map<String, String> httpConfig) {
        this.httpConfig = httpConfig;
    }
}
