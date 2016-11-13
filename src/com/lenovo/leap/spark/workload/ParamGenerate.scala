package com.lenovo.leap.spark.workload

object ParamGenerate {
  def main(args: Array[String]){
    val numNodes = 10
    val memoryPerNode = 109
    val coresPerNode = 40
    var coresPerExecutor = 0
    
    var params = Map[String,String]()
    for (coresPerExecutor <- 5 to 5) {
     
      val executorsPerNode = coresPerNode / coresPerExecutor
      val numExecutors = executorsPerNode * numNodes -1
      val memoryPerExecutor =  Math.floor((memoryPerNode / executorsPerNode) * .93).toInt
      params = params + ("spark.executor.instances" -> numExecutors.toString())
      params = params + ("spark.executor.memory" -> (memoryPerExecutor.toString() + "G"))
      params = params + ("spark.executor.cores" -> coresPerExecutor.toString())
      
      params.foreach(map => println(map._1 + ":" + map._2))
      println()
      //Thread.sleep(5000L)
    }
  }
}
  