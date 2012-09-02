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

import com.excilys.ebi.gatling.core.config.GatlingFiles
import com.excilys.ebi.gatling.core.feeder.SourceBasedFeederBuilder
import com.excilys.ebi.gatling.core.util.FileHelper.{ COMMA_SEPARATOR, SEMICOLON_SEPARATOR, TABULATION_SEPARATOR }

object SeparatedValuesFeederBuilder {
	def csv(fileName: String, escapeChar: Option[String] = None) = new SeparatedValuesFeederBuilder(fileName, COMMA_SEPARATOR, escapeChar)
	def tsv(fileName: String, escapeChar: Option[String] = None) = new SeparatedValuesFeederBuilder(fileName, TABULATION_SEPARATOR, escapeChar)
	def ssv(fileName: String, escapeChar: Option[String] = None) = new SeparatedValuesFeederBuilder(fileName, SEMICOLON_SEPARATOR, escapeChar)
}

class SeparatedValuesFeederBuilder(fileName: String, separator: String, escapeChar: Option[String] = None) extends SourceBasedFeederBuilder[SeparatedValuesFeederSource] {
	protected lazy val source = new SeparatedValuesFeederSource(GatlingFiles.dataDirectory / fileName, separator, escapeChar)
}
