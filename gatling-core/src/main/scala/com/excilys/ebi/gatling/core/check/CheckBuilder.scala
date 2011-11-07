package com.excilys.ebi.gatling.core.check
import com.excilys.ebi.gatling.core.log.Logging

trait CheckBuilder[WHERE] extends Logging {
	def build: Check[WHERE]
}