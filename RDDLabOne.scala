//Created by Zhiyue Zhang, 19-09-2019
package lab1

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession

import scala.collection.mutable

object RDDLabOne {

  def main(args: Array[String]) {



    val t0 = System.nanoTime()
    process()
    val t1 = System.nanoTime()
    println("Total time:" + (t1-t0) + "ns")
/*
    val zeroValue = new mutable.HashMap[String, Int]() {
      override def default(key: String) = 0
    }
*/

  }

  def process(){
    val spark = SparkSession
      .builder
      .appName("Lab1")
   //   .master("local[*]")
      .getOrCreate()
    val sc = spark.sparkContext
    val rdd = sc
      //.textFile("data/segment/*.csv")
      .textFile("s3://gdelt-open-data/v2/gkg/*.gkg.csv")
      //.textFile("s3://gdelt-open-data/v2/gkg/20150218230000.gkg.csv")
     // .coalesce(8)


    val rawData = rdd
      .filter(row => row.split("\t", -1).length == 27)
      //.filter(row => row.split("\t").length == 27)
      .flatMap(row => {
        val columns = row.split("\t", -1)
        //val columns = row.split("\t")
        val date = columns(1).substring(0, 8)
        columns(23)
          .split(";", -1)
          //.split(";")
          .map(names => {
            val name = names.split(",")(0)
            (date, name)
          })
          .filter(x => x._2 != "" && x._2 != "Type ParentCategory")
      })

    val newestResult = rawData
      .aggregateByKey(new mutable.HashMap[String, Int]() {
        override def default(key: String) = 0
      })(seqFunc, combFunc)
      .mapValues(value => value
        .toList
        .sortBy(-_._2)
        .take(10))

    // Save

    //newestResult.saveAsTextFile("./testingData_rdd")
    newestResult.foreach(println)
    //
    //data.saveAsTextFile("./output_rdd")
    //Created by Zhiyue Zhang
    // Quit
    spark.stop()


  }


  def seqFunc(accNum: mutable.HashMap[String, Int], key: String): mutable.HashMap[String, Int] = {
     val keyValueNum = (key -> (accNum(key) + 1))
    accNum  += keyValueNum
  }

  def combFunc(count1: mutable.HashMap[String, Int], count2: mutable.HashMap[String, Int]): mutable.HashMap[String, Int] = {
    count2.foreach {
      case (k, v) => count1 += (k -> (count1(k) + v))
    }
    count1
  }

}