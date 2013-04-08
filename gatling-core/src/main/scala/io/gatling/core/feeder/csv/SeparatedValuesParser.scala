/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.core.feeder.csv

import scala.io.Source
import scala.tools.nsc.io.Path

import io.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import io.gatling.core.feeder.{ AdvancedFeederBuilder, Record }
import io.gatling.core.util.FileHelper.{ commaSeparator, semicolonSeparator, tabulationSeparator }
import io.gatling.core.util.IOHelper.withSource

object SeparatedValuesParser {

	def parse(file: Path, separator: String, escapeChar: Option[String] = None): Array[Record[String]] = {

		def readLine(line: Array[String]) = escapeChar.map(escape => line.map(_.stripPrefix(escape).stripSuffix(escape))).getOrElse(line)

		require(file.exists, s"file $file doesn't exists")

		withSource(Source.fromFile(file.jfile, GatlingConfiguration.configuration.core.encoding)) { source =>

			val rawLines = source.getLines.map(_.split(separator)).map(readLine)
			val headers = rawLines.next

			rawLines.map(line => headers.zip(line).toMap).toArray
		}
	}

	def csv(fileName: String, escapeChar: Option[String]): AdvancedFeederBuilder[String] = csv(GatlingFiles.feederFile(fileName), escapeChar)
	def csv(file: Path, escapeChar: Option[String]) = AdvancedFeederBuilder(parse(file, commaSeparator, escapeChar))

	def tsv(fileName: String, escapeChar: Option[String]): AdvancedFeederBuilder[String] = tsv(GatlingFiles.feederFile(fileName), escapeChar)
	def tsv(file: Path, escapeChar: Option[String]) = AdvancedFeederBuilder(parse(file, tabulationSeparator, escapeChar))

	def ssv(fileName: String, escapeChar: Option[String]): AdvancedFeederBuilder[String] = ssv(GatlingFiles.feederFile(fileName), escapeChar)
	def ssv(file: Path, escapeChar: Option[String]) = AdvancedFeederBuilder(parse(file, semicolonSeparator, escapeChar))
}