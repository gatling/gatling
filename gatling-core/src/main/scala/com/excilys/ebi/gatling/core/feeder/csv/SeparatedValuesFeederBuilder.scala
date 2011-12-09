package com.excilys.ebi.gatling.core.feeder.csv

import com.excilys.ebi.gatling.core.util.FileHelper._
import com.excilys.ebi.gatling.core.feeder.QueueFeeder
import com.excilys.ebi.gatling.core.feeder.FeederBuilder

object SeparatedValuesFeederBuilder {
	def csv(fileName: String) = new SeparatedValuesFeederBuilder(fileName, COMMA_SEPARATOR)
	def tsv(fileName: String) = new SeparatedValuesFeederBuilder(fileName, TABULATION_SEPARATOR)
	def ssv(fileName: String) = new SeparatedValuesFeederBuilder(fileName, SEMICOLON_SEPARATOR)
}
class SeparatedValuesFeederBuilder(fileName: String, separator: String) extends FeederBuilder[SeparatedValuesFeederSource] {
	def sourceInstance = new SeparatedValuesFeederSource(fileName, separator)
}
