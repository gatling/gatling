package com.excilys.ebi.gatling.core.check

trait CheckStrategy {

	def apply(value: List[String], expected: List[String]): Boolean
}