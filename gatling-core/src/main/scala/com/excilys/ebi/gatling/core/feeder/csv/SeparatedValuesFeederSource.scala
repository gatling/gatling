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
import scala.io.Source
import com.excilys.ebi.gatling.core.config.GatlingFiles._
import com.excilys.ebi.gatling.core.config.GatlingConfig._
import com.excilys.ebi.gatling.core.feeder.FeederSource
import com.excilys.ebi.gatling.core.util.PathHelper.path2string

class SeparatedValuesFeederSource(fileName: String, separator: String) extends FeederSource(fileName) {

	val lines = Source.fromFile(GATLING_DATA_FOLDER / fileName, CONFIG_ENCODING).getLines

	val headers = lines.next.split(separator).toList

	val values = lines.filterNot(_.isEmpty).map { line =>
		(headers zip line.split(separator).toList).toMap[String, String]
	}.toBuffer
}