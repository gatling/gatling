package com.excilys.ebi.gatling.core.feeder.csv

import com.excilys.ebi.gatling.core.util.FileHelper._
import com.excilys.ebi.gatling.core.feeder.QueueFeeder
import com.excilys.ebi.gatling.core.feeder.FeederBuilder

object SeparatedValuesFeederBuilder {
	def csv(fileName: String) = new SeparatedValuesFeederBuilder(fileName, CSV_EXTENSION, COMMA_SEPARATOR)
	def tsv(fileName: String) = new SeparatedValuesFeederBuilder(fileName, TSV_EXTENSION, TABULATION_SEPARATOR)
	def ssv(fileName: String) = new SeparatedValuesFeederBuilder(fileName, SSV_EXTENSION, SEMICOLON_SEPARATOR)
}
class SeparatedValuesFeederBuilder(fileName: String, extension: String, separator: String) extends FeederBuilder[SeparatedValuesFeederSource] {
	def sourceInstance = new SeparatedValuesFeederSource(fileName, extension, separator)
}
