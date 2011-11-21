package com.excilys.ebi.gatling.charts.series
import org.joda.time.DateTime

object SharedSeries {

	private var allActiveSessionsSeries: Option[Series[DateTime, Int]] = None

	def getAllActiveSessionsSeries = allActiveSessionsSeries.getOrElse(throw new IllegalArgumentException("Active sessions series was not set yet"))

	def setAllActiveSessionsSeries(allActiveSessionsSeries: Series[DateTime, Int]) = {
		this.allActiveSessionsSeries = Some(allActiveSessionsSeries)
	}
}