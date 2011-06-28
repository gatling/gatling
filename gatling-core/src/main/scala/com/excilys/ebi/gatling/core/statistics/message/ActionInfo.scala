package com.excilys.ebi.gatling.core.statistics.message

case class ActionInfo(val scenarioName: String, val userId: Integer, val actionSummary: String, val executionTime: Long)