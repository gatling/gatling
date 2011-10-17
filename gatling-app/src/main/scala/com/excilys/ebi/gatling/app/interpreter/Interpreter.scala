package com.excilys.ebi.gatling.app.interpreter

import com.excilys.ebi.gatling.core.log.Logging
import org.joda.time.DateTime

trait Interpreter extends Logging {
	def run(fileName: String, startDate: DateTime)
}