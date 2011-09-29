package com.excilys.ebi.gatling.core.result.message

import org.joda.time.DateTime

case class ActionInfo(val scenarioName: String, val userId: Int, val action: String, val executionStartDate: DateTime, val executionDuration: Long, val resultStatus: ResultStatus.ResultStatus, val resultMessage: String)