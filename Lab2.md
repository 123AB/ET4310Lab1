# Super Computing for Big Data (ET4310) - Lab 2
##### Zhiyue Zhang (4764242) and N.U.S.Zia (4822498)
##### October 12, 2019

## Introduction

This report represent the implementation of running a Spark application on the Amazon Web Service
(AWS). We will analyze a large open data set by using Apache Spark and the Amazon Web Services 
(AWS). The data set is the GDELT 2.0 Global Knowledge Graph (GKG), which indexes persons, 
organizations, companies, locations, themes, and even emotions from live news reports in print, broadcast 
and internet sources all over the world. We will use this data to construct a rank of the topics that are most popular on a given day, expected giving us some interesting insights such as future trend  based on the past popular topics discussed in news articles in recent history. We are
interested in one column of the data, the ALLNAMES field. This field contains all proper names
referenced in the news articles, such as people, organization, events, movements, wars, and named
legislation2
, which we use as the topics.
To be more concrete, we retrieve the top 10 mentioned topics of each day by counting the
terms in the ALLNAMES fields and aggregate them by date. We are using RDD implementation in this lab.

## Configuration
We start with our original RDD implementation, which runs faster on our local machine than the
Dataset implementation. The configuration and runtime on a subset of the dataset is shown in
Table 1.

Table 1: Run time on subsets of the dataset with a cluster.

|   |# instance i| instance type  | memory (GiB)  |Storage  | # vCPU   |Entire dataset| 100 data files |
|---|---|---|---|---|---|---|---|
| Master  |1    |c4.8xlarge   | 60  |512    |720   |17min   |19s |
|  Core |20   |c4.8xlarge   |60    |512    |36   |17 min   |19s |

We can see that with the time increase the process performance better than linear growth with respect to the number of files, it seems
to scale well. And we did not got any error for this settings.

Table 2: Settings of a cluster with 1 m4.xlarge master node and 20 m4.4xlarge core nodes.

|Setting   |Description   |Default   |Apply config   |
|---|---|---|---|
|spark.driver.memory   |Amount of memory to use for the driver process.   |10G   |10G   |
|spark.executor.memory    |Amount of memory to use per executor process.  |10G   |10G   |
|spark.executor.cores   |# cores to use on each executor   |2   |20   |
|spark.executor.instances   |# executors   |20   |20   |
|spark.default.parallelism    |# partitions in RDDs    |# cores on all executor   |370   |

## Modification
At this stage,  we set the following values in this case: driver memory, executor memory, number of executors, number of cores
used in each executor, and RDD parallelism. We set the number of
executor.cores is 10 times the number of vCPUs, and the parallelism is 20 times as we expected.

With this setting, we were able to process the entire dataset with a significant performance
decrease. The configurations (Config) and the runtime are as follows:
• Config 1: 1 master (c4.8xlarge) and 20 cores (c4.8xlarge) 20 executor spark.default.parallelism=370 
Time: 17:00
• Config 2: 1 master (m4.xlarge) and 16 cores (m4.4xlarge)
Time: 
• Config 3: 1 master (c4.8xlarge) 15 cores (c4.8xlarge) 200 executor spark.default.parallelism=400
Time: 5
To this end, we have successfully met the requirement of running the application within half an
hour using 20 c4.8xlarge instances. However, the cluster configuration is chosen quite arbitrarily
and requires some justification. The following figures shows some graphs we get from Apache Ganglia for the Config 1. The time when each stage is being executed is shown in the bottom graph. The graphs represent the CPU usage of each instance in
the cluster, the CPU usage of the cluster, the memory usage of the cluster, and the network of
the cluster (left to right, top to bottom).

We can observe a few things:
1. The CPU utilization is very low around 50%.
2. The network bandwidth, in this case around 10 GB/s, is a bottleneck when reading the
input files, and the output is quite slow compare with the input speed.
3. Memory capacity can possibly be reduced as the usage is less than 60% even at its peak.
In the following, we will optimize the application based on these observations.


## Improvements

### Tuning instance type and numbers

Table 3: Summaries of the execution time and some Ganglia metrics of different configurations

|Config  |Settings   |Exe. Time 1st Stage(min)|Exe. Time 2nd Stage(min)|Total Time(min:sec)|Max CPUusage (%)|Max NetworkBW (GB/s)|
|---|---|---|---|---|---|---|
|1   |   |   |   |   |   |   |
|2   |   |   |   |   |   |   |
|3   |   |   |   |   |   |   |
|4   |   |   |   |   |   |   |
|5   |   |   |   |   |   |   |
|6   |   |   |   |   |   |   |


### Modifying the application

### Tuning Yarn/Spark configuration flags

## Recommendation of Configuration

## Conclusion

## Future Improvement

Table 4: Memory, CPUs, storage, network, and the costs of several instance types that we use.

|Instance Types   |Memory   |vCPUs   |Storage   |Network   | On-Demand Cost  |
|---|---|---|---|---|---|
|   |   |   |   |   |   |
|   |   |   |   |   |   |
|   |   |   |   |   |   |
|   |   |   |   |   |   |
|   |   |   |   |   |   |
|   |   |   |   |   |   |
|   |   |   |   |   |   |
|   |   |   |   |   |   |
|   |   |   |   |   |   |
