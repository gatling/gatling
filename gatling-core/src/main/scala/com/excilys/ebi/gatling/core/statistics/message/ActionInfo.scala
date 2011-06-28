package com.excilys.ebi.gatling.core.statistics.message

case class ActionInfo(val userId: Integer, val action: String, val executionStartTime: Long, val executionDuration: Long, val result: String)