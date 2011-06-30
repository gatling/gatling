package com.excilys.ebi.gatling.core.result.message

import java.util.Date

case class ActionInfo(val userId: Int, val action: String, val executionStartDate: Date, val executionDuration: Long, val result: String)