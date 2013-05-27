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
package com.excilys.ebi.gatling.core.feeder.csv

import scala.io.Source
import scala.tools.nsc.io.Path

import com.excilys.ebi.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import com.excilys.ebi.gatling.core.util.FileHelper.{ COMMA_SEPARATOR, SEMICOLON_SEPARATOR, TABULATION_SEPARATOR }
import com.excilys.ebi.gatling.core.util.IOHelper.use

import au.com.bytecode.opencsv.CSVParser

object SeparatedValuesParser {

	def csv(fileName: String): Array[Map[String, String]] = csv(GatlingFiles.dataDirectory / fileName)
	def csv(file: Path): Array[Map[String, String]] = apply(file, COMMA_SEPARATOR.charAt(0))

	def tsv(fileName: String): Array[Map[String, String]] = tsv(GatlingFiles.dataDirectory / fileName)
	def tsv(file: Path): Array[Map[String, String]] = apply(file, TABULATION_SEPARATOR.charAt(0))

	def ssv(fileName: String): Array[Map[String, String]] = ssv(GatlingFiles.dataDirectory / fileName)
	def ssv(file: Path): Array[Map[String, String]] = apply(file, SEMICOLON_SEPARATOR.charAt(0))

	def apply(file: Path, separator: Char): Array[Map[String, String]] = {

		require(file.exists, "file " + file + " doesn't exists")

		use(Source.fromFile(file.jfile, GatlingConfiguration.configuration.core.encoding)) { source =>

			val parser = new CSVParser(separator)
			val rawLines = source.getLines.map(parser.parseLine)
			val headers = rawLines.next

			rawLines.map(headers.zip(_).toMap).toArray
		}
	}
}