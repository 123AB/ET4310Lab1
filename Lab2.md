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


## Cost
We use cost as our metric for picking the best cluster configuration. We define cost as:

Cost = t (M + nC)

where 

t = Time taken to complete the step

M = master node cost

n = core instances

C = core node cost

*cost for each node is sum of amazon EMR and EC2 cost [1]


## Configuration
To calculate spark submit parameters we used the guidelines provided in [2]. 
`**executor-cores**` = *Number of cores = Concurrent tasks an executor can run =*  5 (recommended).
Number of executors per node = Total cores / `executor-cores`

`**num-executors**` =(  core_instances * (Total available cores in one node) / `executor-cores`) - 1*
**1 is subtracted as there is 1 executor (java process) for Application Master in YARN.*

`**executor-memory**` = (memory for each executor in each node) - 0.07*overhead
*where overhead is  max(384, .07 * spark.executor.memory)*

`**spark.default.parallelism**` -  `**num-executors**` x `**executor-cores**`

The values will always be rounded down.
For example for a 16 vCore, 32 GiB memory node (**m4.2xlarge)**, the parameters will be
--num-executors 29 --executor-cores 5 --executor-memory 9G  --conf spark.default.parallelism=145

## Baseline

We use 20 c4.8xlarge core nodes and one master node as our baseline. The time taken to process entire dataset was less than 5 minutes and cost was 3.19$. 

The configuration closen for baseline were:

|Setting   |Description   |Default   |Apply config   |
|---|---|---|---|
|spark.driver.memory   |Amount of memory to use for the driver process.   |10G   |10G   |
|spark.executor.memory    |Amount of memory to use per executor process.  |10G   |7G   |
|spark.executor.cores   |# cores to use on each executor   |2   |5   |
|spark.executor.instances   |# executors   |20   |139   |
|spark.default.parallelism    |# partitions in RDDs    |# cores on all executor   |695   |

## Results (Noor)
| Master          | Core                        | Time    | Cost per instance (EC2, EMR, EC2, EMR) | Cost     |
| --------------- | --------------------------- | ------- | -------------------------------------- | -------- |
| 1 c4.8xlarge    | 20 c4.8xlarge               | 294     | 1.591, 0.27, 1.591, 0.27               | 3.19     |
| 1 m4.xlarge     | 3 c5.18xlarge               | 844     | 0.2 , 0.06, 3.06, 0.27                 | 2.40     |
| 1 m4.xlarge     | 5 m4.10xlarge               | 1038    | 0.2, 0.06, 2, 0.27                     | 3.34     |
| 1 m4.xlarge     | 5 c5.9xlarge                | 978     | 0.2 , 0.06, 1.53, 0.27                 | 2.51     |
| 1 m4.large      | 5 c5.9xlarge                | 1028    | 0.1,  0.03, 1.53, 0.27                 | 2.60     |
| 1 c5.18xlarge   | 5 c5.18xlarge               | 522     | 3.06, 0.27,  3.06, 0.27                | 2.89     |
| 1 c5.18xlarge   | 10 c5.18xlarge              | 296     | 3.06, 0.27, 3.06 , 0.27                | 3.01     |
| 1 m4.xlarge     | 5 c5.18xlarge               | 510     | 0.2 , 0.06, 3.06, 0.27                 | 2.39     |
| **1 m4.xlarge** | **5 c5.18xlarge (4)** | **498** | **0.2, 0.06, 3.06, 0.27**              | **2.33** |
| 1 m4.2xlarge    | 20 m4.2xlarge               | 2086    | 0.4, 0.12, 0.4, 0.12                   | 6.32     |
| 1 m4.xlarge     | 3 c5.9xlarge                | 1566    | 0.2, 0.06, 1.53, 0.27                  | 2.46     |


## Modification
At this stage,  we set the following values in this case: driver memory, executor memory, number of executors, number of cores
used in each executor, and RDD parallelism. We set the number of
executor.cores is 10 times the number of vCPUs, and the parallelism is 20 times as we expected.

With this setting, we were able to process the entire dataset with a significant performance
decrease. The configurations (Config) and the runtime are as follows:
• Config 1: 1 master (c4.8xlarge) and 20 cores (c4.8xlarge) 20 executor spark.default.parallelism=370 
Time: 17:00
• Config 2: 1 master (m4.xlarge) and 16 cores (m4.4xlarge)
Time: 31:00
• Config 3: 1 master (c4.8xlarge) 15 cores (c4.8xlarge) 200 executor spark.default.parallelism=400
Time: 5:00
To this end, we have successfully met the requirement of running the application within half an
hour using 20 c4.8xlarge instances. However, the cluster configuration is chosen quite arbitrarily
and requires some justification. The following figures shows some graphs we get from Apache Ganglia for the Config 1. The time when each stage is being executed is shown in the bottom graph. The graphs represent the CPU usage of each instance in
the cluster, the CPU usage of the cluster, the memory usage of the cluster, and the network of
the cluster (left to right, top to bottom). And another things is the m4.xlarge did not meet our requirment because of the lack of cores.

We can observe a few things:
1. The CPU utilization is very low around 50%.
2. The network bandwidth, in this case around 10 GB/s, is a bottleneck when reading the
input files, and the output is quite slow compare with the input speed.
3. Memory capacity can possibly be reduced as the usage is less than 60% even at its peak.
In the following, we will optimize the application based on these observations.


## Improvements
In this section, we will discuss the configurations that we have experimented with based on previous
observations as well as new observations along the way. But before diving into all the experiments,
we also have optimized the application code itself. The application was output the file but we cancel this step try to reduce our running time and the network workload.
### Tuning instance type and numbers
We mainly use compute-optimized instances as the memory requirement of the application is not
a bottleneck. C5 is the newest generation of the compute-optimized family and is said to provide
improved processing power at a lower cost [4]. Thus we mainly experiment with the C5 family.
Following are the motivation for our experiments:
• Config 3: Compare whether C4 and C5 family has an actual difference.
• Config 4: Test whether m4.xlarge is sufficient as a master node; test whether c5.4xlarge
would double execution time compared to c5.9xlarge.
• Config 5: The hypothesis here is that using a few large nodes can reduce the amount of data
transferred between nodes. We assume that data transfer between CPUs within a node is
faster.
• Config 6: Test the effect of many small nodes.

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
Finally, we want to point to some possible improvement plans.
1. The network become our bottleneck and in the future with the 5G development, the testing performance would be better if the hardware stteing keeeps same.
2. As the dataset increases, the amount of data being shuffled would increase. Therefore our code for the spark and scala mapreduce operation still need to be improved to reduce the running time.
3. Write a script to automate the all test settings to reduce the time cost.

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

## References
[1] https://aws.amazon.com/emr/pricing/

[2] http://site.clairvoyantsoft.com/understanding-resource-allocation-configurations-spark-application/
