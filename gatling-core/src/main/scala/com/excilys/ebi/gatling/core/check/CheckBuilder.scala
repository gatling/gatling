package com.excilys.ebi.gatling.core.check
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.check.strategy.CheckStrategy
import com.excilys.ebi.gatling.core.context.Context

abstract class CheckBuilder[WHERE](val what: Context => String, val to: Option[String], val strategy: CheckStrategy, val expected: Option[String]) extends Logging {
	def build: Check[WHERE]
}