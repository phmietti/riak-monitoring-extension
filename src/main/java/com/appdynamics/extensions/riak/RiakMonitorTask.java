package com.appdynamics.extensions.riak;


import com.appdynamics.extensions.http.Response;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.riak.config.Server;
import com.appdynamics.extensions.util.MetricUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.type.TypeReference;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

public class RiakMonitorTask implements Callable<RiakMetrics> {

    private Server server;
    private Set<String> metrics;
    private SimpleHttpClient httpClient;
    private boolean useSSL;
    public static final Logger logger = Logger.getLogger(RiakMonitorTask.class);

    public RiakMonitorTask(Server server, SimpleHttpClient httpClient,Set<String> metrics,boolean useSSL) {
        this.server = server;
        this.metrics = metrics;
        this.httpClient = httpClient;
        this.useSSL = useSSL;
    }


    public RiakMetrics call() throws Exception {
        RiakMetrics metrics = new RiakMetrics();
        metrics.setDisplayName(server.getDisplayName());
        try{
            Response response = httpClient.target(constructUrl()).get();
            Map<String,Object> allMetrics = response.json(Map.class);
            if(allMetrics != null){
                //filter metrics
                Map<String,String> filteredMetrics = filterMetrics(allMetrics);
                filteredMetrics.put(RiakMonitorConstant.HEALTH_CHECK,RiakMonitorConstant.HEALTH_OK);
                metrics.setMetrics(filteredMetrics);
            }
        }
        catch(Exception e){
            logger.error("Error while collecting the metrics for :: " + metrics.getDisplayName() + e);
            metrics.getMetrics().put(RiakMonitorConstant.HEALTH_CHECK,RiakMonitorConstant.HEALTH_NOT_OK);
        }

        return metrics;
    }

    private Map<String,String> filterMetrics(Map<String, Object> allMetrics) {
        Map<String,String> filteredMetrics = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : allMetrics.entrySet()){
            String key = entry.getKey();
            Object value = entry.getValue();
            if(metrics.contains(key) && value instanceof Number){
                if (logger.isDebugEnabled()) {
                    logger.debug("Metric key:value before rounding off = "+ key + ":" + String.valueOf(value));
                }
                String attribStr = MetricUtils.toWholeNumberString(value);
                filteredMetrics.put(key,attribStr);
            }
        }
        return filteredMetrics;
    }



    private String constructUrl() {
        String scheme = "http://";
        if(useSSL){
            scheme = "https://";
        }
        return scheme + server.getHost() + ":" + server.getPort() + "/" + "stats";
    }
}
