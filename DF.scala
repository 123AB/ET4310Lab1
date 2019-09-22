package DF

import org.apache.spark.sql.types._
import org.apache.spark.sql._
import java.sql.Timestamp
import java.time.format.DateTimeFormatter
import org.apache.spark.sql.functions._
import org.apache.log4j.{Level, Logger}

object DFspark {

  // class object
  case class DFData(
    GKGrecordID : String,
    GKdate : Timestamp,
    SourceCollectionIdentifier: Integer,
    SourceCommonName : String ,
    DocumentIdentifier: String,
    Counts : String, 
    V2Counts : String, 
    Themes : String, 
    V2Themes : String, 
    Locations : String,
    V2Locations: String,
    Persons: String,
    V2Persons: String,
    Organizations: String,
    V2Organizations: String, 
    V2Tone : String,
    Dates: String,
    GCAM: String,
    SharingImage: String,
    RelatedImages: String,
    SocialImageEmbeds: String,
    SocialVideoEmbeds: String,
    Quotations: String,
    AllNames: String,
    Amounts: String,
    TranslationInfo: String,
    Extras: String
  )

  def main(args: Array[String]){

    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)

    //schema based on the headers
    val schema = StructType(
      Array(
        StructField("GKGrecordID", StringType, nullable=true),
        StructField("GKdate", TimestampType, nullable=true),
        StructField("SourceCollectionIdentifier", IntegerType, nullable=true),
        StructField("SourceCommonName", StringType, nullable=true),
        StructField("DocumentIdentifier", StringType, nullable=true),
        StructField("Counts", StringType, nullable=true),
        StructField("V2Counts", StringType, nullable=true),
        StructField("Themes", StringType, nullable=true),
        StructField("V2Themes", StringType, nullable=true),
        StructField("Locations", StringType, nullable=true),
        StructField("V2Locations", StringType, nullable=true),
        StructField("Persons", StringType, nullable=true),
        StructField("V2Persons", StringType, nullable=true),
        StructField("Organizations", StringType, nullable=true),
        StructField("V2Organizations", StringType, nullable=true),
        StructField("V2Tone", StringType, nullable=true),
        StructField("Dates", StringType, nullable=true),
        StructField("GCAM", StringType, nullable=true),
        StructField("SharingImage", StringType, nullable=true),
        StructField("RelatedImages", StringType, nullable=true),
        StructField("SocialImageEmbeds", StringType, nullable=true),
        StructField("SocialVideoEmbeds", StringType, nullable=true),
        StructField("Quotations", StringType, nullable=true),
        StructField("AllNames", StringType, nullable=true),
        StructField("Amounts", StringType, nullable=true),
        StructField("TranslationInfo", StringType, nullable=true),
        StructField("Extras", StringType, nullable=true)
      )
    )

    // initialize spark session
    val spark = SparkSession
      .builder
      .appName("DF")
      .config("spark.master", "local[8]")
      .getOrCreate()

    // initialize spark context
    val sc = spark.sparkContext 
    import spark.implicits._

    // read data from CSV
    val ds = spark.read.format("csv")
      .option("delimiter", "\t")
      .option("timestampFormat", "yyyyMMddHHmmSS")
      .schema(schema)
      .load("./data/segment10/*.csv")
      .as[DFData]

    // remove full instances
    val dsFilter = ds.filter(a => a.AllNames != null)
                  .filter(a=> a.GKdate != null)

    // remove extra columns and remove names that contain Parent category by filtering category
    val time_name = dsFilter.map(x => (x.GKdate,x.AllNames))
                    .filter(x => !(x._2.contains("Category")))

    // format time stamp to date format and split the names column
    val date_names = time_name.map(x => (x._1.toLocalDateTime.format(DateTimeFormatter.ofPattern("dd-MM-YYYY")), x._2.split(",[0-9;]+")))

    // map split names into individual rows and count
    val time_name_n = date_names.flatMap(x => x._2.map(name => ((x._1, name))))
                      .groupBy($"_1", $"_2")
                      .count()

    // sort the data based on decreasing frequency
    val time_name_sort = time_name_n.sort($"count".desc)

    // create a new column that contains the (name, count) pairs belonging to same date
    val time_names_list = time_name_sort.withColumn("list", struct($"_2", $"count")).groupBy("_1")
                          .agg(collect_list("list") as "list")  

    // print the top 10 names for each date 
    for (x <- time_names_list.collect ){
      val y = x.get(1).asInstanceOf[Seq[(String,Int)]].take(10)
      printf("[%s : [%s] ]\n", x.get(0), y)

    } 
    spark.stop

  }
}