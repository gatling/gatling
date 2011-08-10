package com.excilys.ebi.gatling.statistics.result

class DetailsRequestsDataResult(val numberOfRequests: Int, val timeValues: List[(String, Double)], val columnData: (List[Double], List[Double]), val min: Double, val max: Double, val medium: Double, val standardDeviation: Double)