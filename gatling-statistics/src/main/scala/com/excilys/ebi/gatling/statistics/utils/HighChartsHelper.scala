package com.excilys.ebi.gatling.statistics.utils
import org.joda.time.format.DateTimeFormat
import com.excilys.ebi.gatling.core.util.DateHelper._

object HighChartsHelper {

	private val highChartsDateTimeFormat = DateTimeFormat.forPattern("'Date.UTC('yyyy', 'MM', 'dd', 'HH', 'mm', 'ss')'")

	def printHighChartsDate(string: String) = highChartsDateTimeFormat.print(parseResultDate(string))
}