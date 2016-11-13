package com.lenovo.leap.spark.workload

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import java.io.FileWriter


object ParallismDriver {
  
  def main(args: Array[String]){
    
    val numNodes = 5
    val memoryPerNode = 107
    val coresPerNode = 20
    var coresPerExecutor = 0
    
    var configs = Map[String,String]()
    val workloads = Map(
                  "SCAN" -> "INSERT OVERWRITE TABLE hibench.scan_uservisits_copy SELECT * FROM hibench.scan_uservisits", 
                  "AGGREGATION" -> "INSERT OVERWRITE TABLE hibench.aggre_uservisits_aggre SELECT sourceIP, SUM(adRevenue) FROM hibench.aggre_uservisits GROUP BY sourceIP",
                  "JOIN" -> "INSERT OVERWRITE TABLE hibench.join_rankings_uservisits_join SELECT sourceIP, avg(pageRank), sum(adRevenue) as totalRevenue FROM hibench.join_rankings R JOIN (SELECT sourceIP, destURL, adRevenue FROM hibench.join_uservisits UV WHERE (datediff(UV.visitDate, '1999-01-01')>=0 AND datediff(UV.visitDate, '2000-01-01')<=0)) NUV ON (R.pageURL = NUV.destURL) group by sourceIP order by totalRevenue DESC")
    
    for (coresPerExecutor <- 6 to 6) {
     
      //val executorsPerNode = coresPerNode / coresPerExecutor
      //val numExecutors = executorsPerNode * numNodes -1
      //val memoryPerExecutor =  Math.floor((memoryPerNode / executorsPerNode) * .93).toInt
      
      val numExecutors = 10
      val memoryPerExecutor = 8
      configs +=  ("spark.executor.instances" -> numExecutors.toString())
      configs += ("spark.executor.memory" -> (memoryPerExecutor.toString() + "G"))
      configs += ("spark.executor.cores" -> coresPerExecutor.toString())
      
      var parallelism = 0
      val paramList = List(30, 60, 120, 180, 240, 300)
      for (parallelism <- paramList) {
        configs += ("spark.default.parallelism" -> parallelism.toString())
        val conf = new SparkConf().setAppName("SparkBench-Parallelism-" + parallelism)
        configs.foreach(kv => conf.set(kv._1, kv._2))
        val spark = SparkSession.builder.config(conf).enableHiveSupport().getOrCreate()
        
        for ( i <- 1 to 3) {
          workloads.foreach {
            pair => val timeTaken = time(exeSql(spark, pair._2))
            val workloadIndex = pair._1 + "-" + parallelism + "-" + i
            writeFile(workloadIndex, timeTaken)
          }
          Thread.sleep(20000L)
        }
        Thread.sleep(20000L)
        spark.stop()
      }      
    }
  }
  
  def exeSql(spark: SparkSession, sql: String): Unit = {
    spark.sql(sql)
  }
  
  def time[R](block: => R): String = {  
    val start = System.nanoTime()
    block    // call-by-name
    val end = System.nanoTime()
    val diff = (end - start)/1e9
    diff.formatted("%.2f")
  }
  def writeFile(workloadIndex: String, timeTaken: String): Unit = {
    val writer = new FileWriter("/root/spark_test/result_data/20160718/report-parallelism.tsv", true)
    try {
        writer.write(workloadIndex + "\t" + timeTaken + "\n")
    }
    finally writer.close()     
  }
}