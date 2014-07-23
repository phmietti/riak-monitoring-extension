package com.appdynamics.extensions.riak;


import com.appdynamics.extensions.PathResolver;
import com.appdynamics.extensions.http.SimpleHttpClient;
import com.appdynamics.extensions.riak.config.ConfigUtil;
import com.appdynamics.extensions.riak.config.Configuration;
import com.appdynamics.extensions.riak.config.Server;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class RiakMonitor extends AManagedMonitor{

    public static final Logger logger = Logger.getLogger(RiakMonitor.class);
    public static final String CONFIG_ARG = "config-file";
    public static final String METRIC_SEPARATOR = "|";
    public static final String LOG_PREFIX = "log-prefix";
    private static final int DEFAULT_NUMBER_OF_THREADS = 10;
    public static final int DEFAULT_THREAD_TIMEOUT = 30;
    private static final int DEFAULT_SOCKET_TIMEOUT = 30;
    private static final int DEFAULT_CONNECT_TIMEOUT = 30;
    public static final String CONNECT_TIMEOUT = "connect-timeout";
    public static final String SOCKET_TIMEOUT = "socket-timeout";
    public static final String USE_SSL = "use-ssl";

    private static String logPrefix;
    private ExecutorService threadPool;
    //To load the config files
    private final static ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();

    public RiakMonitor(){
        String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    @Override
    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if(taskArgs != null){
            setLogPrefix(taskArgs.get(LOG_PREFIX));
            logger.info(getLogPrefix() + "Starting the RiakMonitor Monitoring task.");
            if (logger.isDebugEnabled()) {
                logger.debug(getLogPrefix() + "Task Arguments Passed ::" + taskArgs);
            }
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try{
                //read the config.
                Configuration config = configUtil.readConfig(configFilename, Configuration.class);
                threadPool = Executors.newFixedThreadPool(config.getNumberOfThreads() == 0 ? DEFAULT_NUMBER_OF_THREADS : config.getNumberOfThreads());
                List<Future<RiakMetrics>> parallelTasks = createConcurrentTasks(config);
                //collect the metrics
                List<RiakMetrics> rMetrics = collectMetrics(parallelTasks,config.getThreadTimeout() == 0 ? DEFAULT_THREAD_TIMEOUT : config.getThreadTimeout());
                //print the metrics
                printStats(config, rMetrics);
                logger.info(getLogPrefix() + "Riak monitoring task completed successfully.");
                return new TaskOutput(getLogPrefix() + "Riak monitoring task completed successfully.");
            } catch (FileNotFoundException e) {
                logger.error(getLogPrefix() + "Config file not found :: " + configFilename, e);
            } catch (Exception e) {
                logger.error(getLogPrefix() + "Metrics collection failed", e);
            }
        }
        throw new TaskExecutionException(getLogPrefix() + "Riak monitoring task completed with failures.");

    }

    private void printStats(Configuration config, List<RiakMetrics> rMetrics) {
        for (RiakMetrics rMetric : rMetrics) {
            StringBuffer metricPath = new StringBuffer();
            metricPath.append(config.getMetricPrefix()).append(rMetric.getDisplayName()).append(METRIC_SEPARATOR);
            Map<String,String> metricsForAServer = rMetric.getMetrics();
            Iterator<String> it = metricsForAServer.keySet().iterator();
            while(it.hasNext()) {
                String metricName = it.next();
                String metricValue = metricsForAServer.get(metricName);
                printAverageAverageIndividual(metricPath.toString() + metricName,metricValue);
            }
        }
    }

    private void printAverageAverageIndividual(String metricPath, String metricValue) {
        printMetric(metricPath, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL
        );
    }

    private void printCollectiveObservedCurrent(String metricPath, String metricValue) {
        printMetric(metricPath, metricValue,
                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                MetricWriter.METRIC_TIME_ROLLUP_TYPE_CURRENT,
                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
    }

    /**
     * A helper method to report the metrics.
     * @param metricPath
     * @param metricValue
     * @param aggType
     * @param timeRollupType
     * @param clusterRollupType
     */
    private void printMetric(String metricPath,String metricValue,String aggType,String timeRollupType,String clusterRollupType){
        MetricWriter metricWriter = getMetricWriter(metricPath,
                aggType,
                timeRollupType,
                clusterRollupType
        );
    //       System.out.println(getLogPrefix()+"Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
    //               + "] metric = " + metricPath + " = " + metricValue);
        if (logger.isDebugEnabled()) {
            logger.debug(getLogPrefix() + "Sending [" + aggType + METRIC_SEPARATOR + timeRollupType + METRIC_SEPARATOR + clusterRollupType
                    + "] metric = " + metricPath + " = " + metricValue);
        }
        metricWriter.printMetric(metricValue);
    }

    /**
     * Creates concurrent tasks
     *
     * @param config
     * @return Handles to concurrent tasks.
     */
    private List<Future<RiakMetrics>> createConcurrentTasks(Configuration config) {
        final SimpleHttpClient simpleHttpClient = buildHttpClient(config);
        boolean useSSL = (config.getHttpConfig().get(USE_SSL) != null) ? Boolean.valueOf(config.getHttpConfig().get(USE_SSL)) : false;
        List<Future<RiakMetrics>> parallelTasks = new ArrayList<Future<RiakMetrics>>();
        if (config != null && config.getServers() != null) {
            for (Server server : config.getServers()) {
                RiakMonitorTask riakTask = new RiakMonitorTask(server,simpleHttpClient,config.getMetrics(),useSSL);
                parallelTasks.add(getThreadPool().submit(riakTask));
            }
        }
        return parallelTasks;
    }

    private SimpleHttpClient buildHttpClient(Configuration config) {
        int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
        int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        try {
            socketTimeout = (config.getHttpConfig().get(SOCKET_TIMEOUT) != null) ? Integer.parseInt(config.getHttpConfig().get(SOCKET_TIMEOUT)) : DEFAULT_SOCKET_TIMEOUT;
            connectTimeout = (config.getHttpConfig().get(CONNECT_TIMEOUT) != null) ? Integer.parseInt(config.getHttpConfig().get(CONNECT_TIMEOUT)) : DEFAULT_CONNECT_TIMEOUT;
        } catch(NumberFormatException nfe){
            logger.error("Improper timeouts in config.yml" + nfe);
        }

        return SimpleHttpClient.builder(config.getHttpConfig())
                .socketTimeout(socketTimeout)
                .connectionTimeout(connectTimeout)
                .build();
    }


    /**
     * Collects the result from the thread.
     *
     * @param parallelTasks
     * @return
     */
    private List<RiakMetrics> collectMetrics(List<Future<RiakMetrics>> parallelTasks, int timeout) {
        List<RiakMetrics> allMetrics = new ArrayList<RiakMetrics>();
        for (Future<RiakMetrics> aParallelTask : parallelTasks) {
            RiakMetrics rMetric = null;
            try {
                rMetric = aParallelTask.get(timeout, TimeUnit.SECONDS);
                allMetrics.add(rMetric);
            } catch (InterruptedException e) {
                logger.error(getLogPrefix() + "Task interrupted." + e);
            } catch (ExecutionException e) {
                logger.error(getLogPrefix() + "Task execution failed." + e);
            } catch (TimeoutException e) {
                logger.error(getLogPrefix() + "Task timed out." + e);
            }
        }
        return allMetrics;
    }


    /**
     * Returns a config file name,
     * @param filename
     * @return String
     */
    private String getConfigFilename(String filename) {
        if(filename == null){
            return "";
        }
        //for absolute paths
        if(new File(filename).exists()){
            return filename;
        }
        //for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if(!Strings.isNullOrEmpty(filename)){
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }

    public void setLogPrefix(String logPrefix) {
        this.logPrefix = (logPrefix != null) ? logPrefix : "";
    }

    public static String getImplementationVersion() {
        return RiakMonitor.class.getPackage().getImplementationTitle();
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public ExecutorService getThreadPool() {
        return threadPool;
    }

}
