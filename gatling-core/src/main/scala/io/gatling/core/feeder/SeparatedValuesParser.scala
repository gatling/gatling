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

import au.com.bytecode.opencsv.CSVParser
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.{ GatlingFiles, Resource }
import io.gatling.core.util.FileHelper.{ commaSeparator, semicolonSeparator, tabulationSeparator }
import io.gatling.core.util.IOHelper.withSource
import io.gatling.core.validation.{ Failure, Success, Validation }

object SeparatedValuesParser {

	def csv(fileName: String): AdvancedFeederBuilder[String] = csv(GatlingFiles.feederResource(fileName))
	def csv(resource: Validation[Resource]) = AdvancedFeederBuilder(parse(resource, commaSeparator.charAt(0)))

	def tsv(fileName: String): AdvancedFeederBuilder[String] = tsv(GatlingFiles.feederResource(fileName))
	def tsv(resource: Validation[Resource]) = AdvancedFeederBuilder(parse(resource, tabulationSeparator.charAt(0)))

	def ssv(fileName: String): AdvancedFeederBuilder[String] = ssv(GatlingFiles.feederResource(fileName))
	def ssv(resource: Validation[Resource]) = AdvancedFeederBuilder(parse(resource, semicolonSeparator.charAt(0)))

	def parse(fileName: String, separator: Char): Array[Record[String]] = parse(GatlingFiles.feederResource(fileName), separator)
	def parse(resource: Validation[Resource], separator: Char): Array[Record[String]] = resource match {

		case Success(res) =>
			withSource(Source.fromInputStream(res.inputStream, configuration.core.encoding)) { source =>

				val parser = new CSVParser(separator)
				val rawLines = source.getLines.map(parser.parseLine)
				val headers = rawLines.next

				rawLines.map(headers.zip(_).toMap).toArray
			}

		case Failure(msg) => throw new IllegalArgumentException(msg)
	}
}