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
package com.excilys.ebi.gatling.core.result.reader

import scala.collection.immutable.SortedMap
import org.joda.time.DateTime
import com.excilys.ebi.gatling.core.result.writer.ResultLine
import com.excilys.ebi.gatling.core.config.GatlingConfig

object DataReader {
	def newInstance(runOn: String) = GatlingConfig.CONFIG_DATA_READER.getConstructor(classOf[String]).newInstance(runOn)
}

abstract class DataReader(runOn: String) {

	val simulationRunOn: DateTime
	val requestNames: Seq[String]
	val scenarioNames: Seq[String]
	val dataIndexedBySendDateWithoutMillis: SortedMap[Long, Seq[ResultLine]]
	val dataIndexedByReceiveDateWithoutMillis: SortedMap[Long, Seq[ResultLine]]

	def requestData(requestName: String): Seq[ResultLine]
	def scenarioData(scenarioName: String): Seq[ResultLine]
	def requestDataIndexedBySendDate(requestName: String): SortedMap[Long, Seq[ResultLine]]
	def requestDataIndexedBySendDateWithoutMillis(requestName: String): SortedMap[Long, Seq[ResultLine]]
	def scenarioDataIndexedBySendDateWithoutMillis(scenarioName: String): SortedMap[Long, Seq[ResultLine]]
}