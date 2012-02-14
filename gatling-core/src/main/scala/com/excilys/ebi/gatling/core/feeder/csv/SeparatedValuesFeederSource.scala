/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
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

import java.io.FileReader

import scala.Array.canBuildFrom
import scala.tools.nsc.io.Path.string2path

import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.feeder.FeederSource
import com.excilys.ebi.gatling.core.util.PathHelper.path2string

import au.com.bytecode.opencsv.CSVReader

class SeparatedValuesFeederSource(fileName: String, separator: Char, escapeChar: Option[Char]) extends FeederSource(fileName) {

	val values = {
		val reader = escapeChar match {
			case Some(char) => new CSVReader(new FileReader(GatlingFiles.dataFolder / fileName), separator, char)
			case None => new CSVReader(new FileReader(GatlingFiles.dataFolder / fileName), separator)
		}

		try {
			val headers = reader.readNext

			new Iterator[Map[String, String]] {

				var line: Array[String] = _

				def hasNext = {
					line = reader.readNext
					line != null
				}

				def next = (headers zip line).toMap[String, String]
			}.toBuffer

		} finally {
			reader.close
		}
	}
}