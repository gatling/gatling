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
package com.excilys.ebi.gatling.core.feeder.csv

import scala.io.Source
import scala.tools.nsc.io.Path

import com.excilys.ebi.gatling.core.config.{ GatlingConfiguration, GatlingFiles }
import com.excilys.ebi.gatling.core.util.FileHelper.{ COMMA_SEPARATOR, SEMICOLON_SEPARATOR, TABULATION_SEPARATOR }
import com.excilys.ebi.gatling.core.util.IOHelper.use

object SeparatedValuesParser {

	def apply(file: Path, separator: String, escapeChar: Option[String]) = new SeparatedValuesParser(file,separator,escapeChar,None)

	def csv(fileName: String, escapeChar: Option[String]):SeparatedValuesParser = csv(GatlingFiles.dataDirectory / fileName, escapeChar)
	def csv(file: Path, escapeChar: Option[String]):SeparatedValuesParser = SeparatedValuesParser(file, COMMA_SEPARATOR, escapeChar)

	def tsv(fileName: String, escapeChar: Option[String]):SeparatedValuesParser = tsv(GatlingFiles.dataDirectory / fileName, escapeChar)
	def tsv(file: Path, escapeChar: Option[String]):SeparatedValuesParser  = SeparatedValuesParser(file, TABULATION_SEPARATOR, escapeChar)

	def ssv(fileName: String, escapeChar: Option[String]):SeparatedValuesParser = ssv(GatlingFiles.dataDirectory / fileName, escapeChar)
	def ssv(file: Path, escapeChar: Option[String]):SeparatedValuesParser = SeparatedValuesParser(file, SEMICOLON_SEPARATOR, escapeChar)

}

class SeparatedValuesParser(file: Path, separator: String, escapeChar: Option[String],conversions : Option[Seq[(String,String => Any)]]) {

	def build: Array[Map[String,Any]] = {

		require(file.exists, s"file $file doesn't exists")

		use(Source.fromFile(file.jfile, GatlingConfiguration.configuration.simulation.encoding)) { source =>

			val rawLines = source.getLines.map(_.split(separator))

			val lines = escapeChar.map { escape =>
				rawLines.map(_.map(_.stripPrefix(escape).stripSuffix(escape)))
			}.getOrElse(rawLines).toArray

			def convert(column: String,content: String) = {
				val conversion = conversions.flatMap(_.find(column == _._1).map(_._2)).getOrElse(identity[String] _)
				conversion(content)
			}

			val headers = lines.head
			lines.tail.map(line => (headers zip line).map{ case (key,value) => (key,convert(key,value))}.toMap)
		}
	}

	def convert(conversions: (String,String => Any)*) = new SeparatedValuesParser(file,separator,escapeChar,Some(conversions))
}