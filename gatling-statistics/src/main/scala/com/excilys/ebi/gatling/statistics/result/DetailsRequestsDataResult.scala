package com.excilys.ebi.gatling.statistics.result

class DetailsRequestsDataResult(val numberOfRequests: Int, val values: List[(String, Double)], val min: Double, val max: Double, val medium: Double, val standardDeviation: Double)