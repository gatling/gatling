package com.excilys.ebi.gatling.core.check.strategy

object ListSizeCheckStrategy extends CheckStrategy {
	def check(value: List[String], expected: List[String]) = value.size == expected(0).toInt
}