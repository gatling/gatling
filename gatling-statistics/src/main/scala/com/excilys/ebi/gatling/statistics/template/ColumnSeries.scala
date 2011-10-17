package com.excilys.ebi.gatling.statistics.template

class ColumnSeries(val name: String, val categories: List[Double], val values: List[Double]) {
	override def toString = {
		"name: '" + name + "', data: " + values.mkString("[ ", ",", " ]")
	}
}