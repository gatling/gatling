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

import com.excilys.ebi.gatling.core.util.FileHelper._
import com.excilys.ebi.gatling.core.feeder.QueueFeeder
import com.excilys.ebi.gatling.core.feeder.FeederBuilder

object SeparatedValuesFeederBuilder {
	def csv(fileName: String, escapeChar : Option[Char] = None) = new SeparatedValuesFeederBuilder(fileName, COMMA_SEPARATOR, escapeChar)
	def tsv(fileName: String, escapeChar : Option[Char] = None) = new SeparatedValuesFeederBuilder(fileName, TABULATION_SEPARATOR, escapeChar)
	def ssv(fileName: String, escapeChar : Option[Char] = None) = new SeparatedValuesFeederBuilder(fileName, SEMICOLON_SEPARATOR, escapeChar)
}
class SeparatedValuesFeederBuilder(fileName: String, separator: Char, escapeChar : Option[Char] = None) extends FeederBuilder[SeparatedValuesFeederSource] {
	def sourceInstance = new SeparatedValuesFeederSource(fileName, separator, escapeChar)
}
