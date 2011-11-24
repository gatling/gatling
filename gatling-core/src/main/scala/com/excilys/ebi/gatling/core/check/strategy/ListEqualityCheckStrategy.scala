package com.excilys.ebi.gatling.core.check.strategy

object ListEqualityCheckStrategy extends CheckStrategy {
	def check(value: List[String], expected: List[String]) = !value.isEmpty && value == expected
}