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

|   |# instance i| instance type  | memory (GiB)  | # vCPU  |   |   |
|---|---|---|---|---|---|---|
| Master  |1    |c4.8xlarge   | 60  |512    |   |   |
|  Core |20   |c4.8xlarge   |60    |512    |36   |   |

Table 2: Settings of a cluster with 1 m4.xlarge master node and 20 m4.4xlarge core nodes.

|Setting   |Description   |Default   |Apply config   |
|---|---|---|---|
|spark.driver.memory   |   |   |   |
|spark.executor.memory    |   |   |   |
|spark.executor.cores   |   |   |   |
|spark.executor.instances   |   |   |   |
|spark.default.parallelism    |   |   |   |

## Modification

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
