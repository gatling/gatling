package com.excilys.ebi.gatling.statistics.result

class DetailsRequestsDataResult(val values: List[(String, Int)], val min: Int, val max: Int, val medium: Double, val standardDeviation: Double) {
  override def toString = "Result: min=" + min + ", max=" + max + ", medium=" + medium + ", std deviation: " + standardDeviation + ", values: " + values
}