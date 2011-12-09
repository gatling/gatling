package com.excilys.ebi.gatling.core.feeder

abstract class FeederSource(val fileName: String) {
	val values: List[Map[String, String]]
}