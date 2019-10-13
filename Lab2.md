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

> Cost = t (M + nC)

where 

t = Time taken to complete the step

M = master node cost

n = core instances

C = core node cost

*cost for each node is sum of amazon EMR and EC2 cost [1]


## Configuration
To calculate spark submit parameters we used the guidelines provided in [2]. 
`**executor-cores**` = *Number of cores = Concurrent tasks an executor can run =*  5 (recommended).
Number of executors per node = (Total cores - 1) / `executor-cores`

`**num-executors**` =(  core_instances * (Total available cores in one node - 1) / `executor-cores`) - 1*
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

The results for baseline were:

| Master          | Core                        | Time    | Cost per instance (EC2, EMR, EC2, EMR)* | Cost     |
| --------------- | --------------------------- | ------- | -------------------------------------- | -------- |
| 1 c4.8xlarge    | 20 c4.8xlarge               | 294     | 1.591, 0.27, 1.591, 0.27               | 3.19     |

* prices mentioned as (EC2 master, EMR master, EC2 core, EMR core) which will be used in the next section as well.

We did test a different value of parallelism but it did not make a significant difference on execution time. Lowering the value resulted in a 6 second improvement only and increasing it increased the time. For further experiments, we did not tune the parallelism parameter and simply calculated it using  `**num-executors**` x `**executor-cores**`.
## Observations
We can observe a few things:

1. The CPU utilization is very low.

2. The network bandwidth, in this case around 10 GB/s, is a bottleneck when reading the
input files, and the output is quite slow compare with the input speed.

3. Memory capacity can possibly be reduced as the usage is less than 50% even at its peak.

In the following, we will optimize the application based on these observations.

## Modifications
We noticed that the memory and CPU utilization was low. We tested switching to a low cost machine (m4.2xlarge) which has 8 vCPU and 8GiB memory. We kept same number of instances (1 master, 20 cores). The time increased by almost 10x and cost nearly doubled. 

We then switched to c5.18xlarge (1 master, 10 cores). The time of execution remained same but the cost decreased slightly as both c4.8xlarge and c5.18xlarge have same EMR cost. 

We notieced that the CPU utilization was low (18%). We switched to 5 c5.18xlarge core instances and the average CPU utilization increased to 40%. 

We also noticed that the master node has very low CPU utilization and it can be replaced with a machine with less vCPUs. We replaced master with m4.xlarge instance and it reduced the cost. We tried switching to a machine with even lesser vCPUs (m4.large) but it increased the cost and execution time.  

| Master          | Core                        | Time    | Cost     |
| --------------- | --------------------------- | ------- | -------- |
| 1 m4.xlarge     | 5 c5.9xlarge                | 978     | 2.51     |
| 1 m4.large      | 5 c5.9xlarge                | 1028    | 2.60     |

We further decreased the core instances to 3, which increased CPU utilization of core nodes to 75-100% range and master node utilization in 0-25% range, and the average utilization is 55%. This combination reduced the cost to **$2.40**.

As the CPU were not being used to full potential with 5 c5.18xlarge, we decided to replace c5 machines with m4.10xlarge. Another reason to do so was the price difference but we did not notice any improvement in cost and the time doubled. 

Memory was not being fully utilized hence we further tested replacing c5.18xlarge instances with c5.9xlarge instances. This did not lead to any improvement as the time of execution doubled and the cost didn't decrease. However, the memory utilization did increase. The results have been summarized in table below.


| Master          | Core                        | Time    | Cost per instance (EC2, EMR, EC2, EMR) | Cost     |
| --------------- | --------------------------- | ------- | -------------------------------------- | -------- |
| 1 c4.8xlarge    | 20 c4.8xlarge               | 294     | 1.591, 0.27, 1.591, 0.27               | 3.19     |
| 1 m4.2xlarge    | 20 m4.2xlarge               | 2086    | 0.4, 0.12, 0.4, 0.12                   | 6.32     |
| 1 c5.18xlarge   | 10 c5.18xlarge              | 296     | 3.06, 0.27, 3.06 , 0.27                | 3.01     |
| 1 c5.18xlarge   | 5 c5.18xlarge      | 522 | 3.06, 0.27,  3.06, 0.27            |2.89 |
| **1 m4.xlarge**     | **5 c5.18xlarge**               | **510**     | **0.2 , 0.06, 3.06, 0.27** | **2.39**     |
| **1 m4.xlarge**     | **3 c5.18xlarge**               | **844**     | **0.2 , 0.06, 3.06, 0.27**  | **2.40** |
| 1 m4.xlarge     | 5 m4.10xlarge               | 1038    | 0.2, 0.06, 2, 0.27                     | 3.34     |
| 1 m4.xlarge     | 5 c5.9xlarge                | 978     | 0.2 , 0.06, 1.53, 0.27                 | 2.51     |
| 1 m4.large      | 5 c5.9xlarge                | 1028    | 0.1,  0.03, 1.53, 0.27                 | 2.60     |
| **1 m4.xlarge**     | **3 c5.9xlarge**                | **1566**    | ***0.2, 0.06, 1.53, 0.27** | **2.46**     |



## Improvement of best setting
We took the best 3 settings we obtained earlier and experimented with the number of executor cores. Setting executor cores to 5 is considered optimal and it is recommended to keep executor cores below 5. But the number can change based on the application. We tested the best 3 settings with executor-core set to 4. We noticed an improvement in results for 5 x5.18xlarge configuration.

| Master          | Core                        | Time    | Cost per instance (EC2, EMR, EC2, EMR) | Cost     |
| --------------- | --------------------------- | ------- | -------------------------------------- | -------- |
| **1 m4.xlarge** | **5 c5.18xlarge (4)** | **498** | **0.2, 0.06, 3.06, 0.27**              | **2.33** |


We did try using Kryo serialization and configured it according to guidelines available in [3]. However, we were not able to get it working with our code. We believe that using it will show some improvement and it is worth investigating. 

## Recommendation of Configuration
We decided to use the cost of the application as the metric to choose our final configuration. The
cost is defined as follows

> Cost = t (M + nC)

Therefore the machine has the minimum cost and relative fast speed is the best machine in this case, so the m4.xlarge (master) and 5 c5.18xlarge (core) with 4 cores is the recommended cluster and costs 2.33 dollars. The cost is calculated by the on-demand cost of Amazon EC2 instances plus the cost of Amazon EMR. We use Spot (max on-demand) due to the fixed prices and easy to compare. Other options which are close in price are:

1. 1 m4.xlarge master, 3 c5.18xlarge core ( $ 2.40)
2. 1 m4.xlarge master, 3 c5.9xlarge ( $ 2.46)

As a result of new configuration, the cost reduced by **$0.86**.

## Conclusion
We were able to successfully process the entire data set within 30 minutes for multiple configurations. The target of our experiments was to find the configuration that processes the entire data set the fastest, and with the least cost, and we think we finsh this target. After that, we also has the following conclusion:

1. For the processing time of second stage we did not observation the impact of Number of parallel. There need to be consider in the future.

2. The master node is not involved in the actual computation, but only responsible for scheduling and monitoring operations.

3. Tuning Spark application is a tedious but important process. Apache Ganglia is really good to monitor the difference between each type of settings.


## Future Improvement
Finally, we want to point to some possible improvement plans.
1. The network become our bottleneck and in the future with the 5G development, the testing performance would be better if the hardware stteing keeeps same.
2. As the dataset increases, the amount of data being shuffled would increase. Therefore our code for the spark and scala mapreduce operation still need to be improved to reduce the running time.
3. Write a script to automate the all test settings to reduce the time cost.


## References
[1] https://aws.amazon.com/emr/pricing/

[2] http://site.clairvoyantsoft.com/understanding-resource-allocation-configurations-spark-application/

[3] https://www.knowru.com/blog/2-tunings-you-should-make-spark-applications-running-emr/
