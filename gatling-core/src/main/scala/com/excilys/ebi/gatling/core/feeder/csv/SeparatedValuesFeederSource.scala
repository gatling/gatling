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

import scala.collection.JavaConversions.asScalaBuffer
import scala.tools.nsc.io.Path.string2path

import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.feeder.FeederSource
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.core.util.PathHelper.path2string

import au.com.bytecode.opencsv.CSVReader

class SeparatedValuesFeederSource(fileName: String, separator: Char, escapeChar: Option[Char]) extends FeederSource(fileName) {

	lazy val values: IndexedSeq[Map[String, String]] = {
		val file = GatlingFiles.dataDirectory / fileName
		if (!file.exists)
			throw new IllegalArgumentException("file " + file + " doesn't exists")

		use(new FileReader(file)) { fileReader =>
			val reader = escapeChar match {
				case Some(char) => new CSVReader(fileReader, separator, char)
				case None => new CSVReader(fileReader, separator)
			}

			val headers = reader.readNext

			reader.readAll.filterNot(line => line.length == 1 && line(0).isEmpty).map(line => (headers zip line).toMap[String, String]).toIndexedSeq
		}
	}
}