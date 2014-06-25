riak-monitoring-extension
=========================

An AppDynamics extension to be used with a stand alone Java machine agent to provide metrics from riak instances.

## Metrics Provided ##
  The metrics provided can be configured in <MACHINE_AGENT_HOME>/monitors/RiakMonitor/

  node_gets, node_gets_total, node_puts,node_puts_total,vnode_gets,vnode_gets_total,vnode_puts_total,memory_processes_used,sys_process_count,
  pbc_connect,pbc_active.

  You can view the names of any other metrics to be configured here http://docs.basho.com/riak/latest/ops/running/stats-and-monitoring/

  We also send a health check metric "Health Check=1"  (when success) or "Health Check=-1" (when failure).

## Installation ##

1. Download and unzip RiakMonitor.zip from AppSphere.
2. Copy the RiakMonitor directory to `<MACHINE_AGENT_HOME>/monitors`.


## Configuration ##

###Note
Please make sure to not use tab (\t) while editing yaml files. You may want to validate the yaml file using a yaml validator http://yamllint.com/

1. Configure the riak instances by editing the config.yaml file in `<MACHINE_AGENT_HOME>/monitors/RiakMonitor/`. Below is the format

    ```
        # List of riak servers
        servers:
          - host: "myDebian.sandbox.appdynamics.com"
            port: 8098
            displayName: myDebian

          - host: "myUbuntu.sandbox.appdynamics.com"
            port: 8098
            displayName: myUbuntu


        metrics: [
          "node_gets",
          "node_gets_total",
          "node_puts",
          "node_puts_total",
          "vnode_gets",
          "vnode_gets_total",
          "vnode_puts_total",
          "memory_processes_used",
          "sys_process_count",
          "pbc_connect",
          "pbc_active"
        ]


        #prefix used to show up metrics in AppDynamics
        metricPrefix:  "Custom Metrics|Riak|"

        # number of concurrent tasks
        numberOfThreads: 10

        #timeout for the thread in seconds
        threadTimeout: 30

        #configuration for making http calls
        httpConfig: {
          use-ssl : false,
          proxy-host : "",
          proxy-port : "",
          proxy-username : "",
          proxy-password : "",
          proxy-use-ssl : "",
          socket-timeout : 10, # in seconds
          connect-timeout : 10 # in seconds
        }


    ```

2. Configure the path to the config.yaml file by editing the <task-arguments> in the monitor.xml file. Below is the sample

     ```
         <task-arguments>
             <!-- config file-->
             <argument name="config-file" is-required="true" default-value="monitors/RiakMonitor/config.yaml" />
              ....
         </task-arguments>

     ```

## Custom Dashboard ##

## Contributing ##

Always feel free to fork and contribute any changes directly via [GitHub][].

## Community ##

Find out more in the [Community][].

## Support ##

For any questions or feature request, please contact [AppDynamics Center of Excellence][].

**Version:** 1.0
**Controller Compatibility:** 3.7 or later
**Riak version tested on:** 1.4.8 

[GitHub]: https://github.com/Appdynamics/riak-monitoring-extension
[Community]: http://community.appdynamics.com/
[AppDynamics Center of Excellence]: mailto:ace-request@appdynamics.com
