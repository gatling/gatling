/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.feeder

import scala.io.Source
import scala.tools.nsc.io.Path

import au.com.bytecode.opencsv.CSVParser
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles
import io.gatling.core.util.FileHelper.{ commaSeparator, semicolonSeparator, tabulationSeparator }
import io.gatling.core.util.IOHelper.withSource

object SeparatedValuesParser {

	def csv(fileName: String): AdvancedFeederBuilder[String] = csv(GatlingFiles.feederFile(fileName))
	def csv(file: Path) = AdvancedFeederBuilder(parse(file, commaSeparator.charAt(0)))

	def tsv(fileName: String): AdvancedFeederBuilder[String] = tsv(GatlingFiles.feederFile(fileName))
	def tsv(file: Path) = AdvancedFeederBuilder(parse(file, tabulationSeparator.charAt(0)))

	def ssv(fileName: String): AdvancedFeederBuilder[String] = ssv(GatlingFiles.feederFile(fileName))
	def ssv(file: Path) = AdvancedFeederBuilder(parse(file, semicolonSeparator.charAt(0)))

	def parse(file: Path, separator: Char): Array[Record[String]] = {

		require(file.exists, s"File $file doesn't exists")

		withSource(Source.fromFile(file.jfile, configuration.core.encoding)) { source =>

			val parser = new CSVParser(separator)
			val rawLines = source.getLines.map(parser.parseLine)
			val headers = rawLines.next

			rawLines.map(headers.zip(_).toMap).toArray
		}
	}
}