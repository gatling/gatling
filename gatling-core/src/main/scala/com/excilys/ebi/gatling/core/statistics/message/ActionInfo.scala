package com.excilys.ebi.gatling.core.statistics.message

import java.util.Date

case class ActionInfo(val userId: Integer, val action: String, val executionStartDate: Date, val executionDuration: Long, val result: String)