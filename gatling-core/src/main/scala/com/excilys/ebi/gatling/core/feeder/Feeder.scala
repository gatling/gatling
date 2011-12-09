package com.excilys.ebi.gatling.core.feeder

abstract class Feeder(feederSource: FeederSource) {
	def next: Map[String, String]
}