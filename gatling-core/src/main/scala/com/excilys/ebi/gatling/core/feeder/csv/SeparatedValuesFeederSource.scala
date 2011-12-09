package com.excilys.ebi.gatling.core.feeder.csv
import scala.io.Source
import com.excilys.ebi.gatling.core.config.GatlingFiles._
import com.excilys.ebi.gatling.core.config.GatlingConfig._
import com.excilys.ebi.gatling.core.feeder.FeederSource

class SeparatedValuesFeederSource(fileName: String, extension: String, separator: String) extends FeederSource(fileName) {

	val lines = Source.fromFile(GATLING_DATA_FOLDER / fileName + extension, CONFIG_ENCODING).getLines

	val headers = lines.next.split(separator).toList

	val values = lines.map { line =>
		(headers zip line.split(separator).toList).toMap[String, String]
	}.toList
}