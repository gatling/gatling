/**
 * Copyright 2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.statistics.extractor

import com.excilys.ebi.gatling.core.config.GatlingConfig._
import com.excilys.ebi.gatling.core.log.Logging
import com.excilys.ebi.gatling.core.util.PathHelper._
import com.excilys.ebi.gatling.core.action.EndAction._

import com.excilys.ebi.gatling.statistics.result.DetailsRequestsDataResult

import scala.io.Source
import scala.collection.immutable.TreeMap
import scala.math._

class DetailsRequestsDataExtractor extends DataExtractor[Map[String, DetailsRequestsDataResult]] with Logging {

	var extractedValues: Map[String, List[(String, Double)]] = TreeMap.empty

	def onRow(runOn: String, scenarioName: String, userId: String, actionName: String, executionStartDate: String, executionDuration: String, resultStatus: String, resultMessage: String, groups: List[String]) {
		if (actionName != END_OF_SCENARIO)
			extractedValues = extractedValues + (actionName ->
				((executionStartDate, executionDuration.toDouble) :: extractedValues.getOrElse(actionName, Nil)))
	}

	def getResults: Map[String, DetailsRequestsDataResult] = {
		var results: Map[String, DetailsRequestsDataResult] = Map.empty
		extractedValues.foreach {
			case (requestName, responseTimes) =>
				val nbOfElements = responseTimes.size.asInstanceOf[Double]
				val min = responseTimes.minBy(_._2)._2
				val max = responseTimes.maxBy(_._2)._2
				val medium = responseTimes.map(entry => entry._2).sum / nbOfElements
				val standardDeviation = sqrt(responseTimes.map(entry => (pow(entry._2 - medium, 2))).sum / nbOfElements)

				var responseTimesToNumberOfRequests: Map[Double, Double] = new TreeMap
				responseTimes.map {
					dateAndResponseTime =>
						responseTimesToNumberOfRequests += (dateAndResponseTime._2 -> (responseTimesToNumberOfRequests.get(dateAndResponseTime._2).getOrElse(0D) + 1))
				}
				val sortedResponseTimes = responseTimesToNumberOfRequests.keys.toList
				val numberOfRequests = responseTimesToNumberOfRequests.values.toList

				val result = new DetailsRequestsDataResult(responseTimes.size, responseTimes.reverse, (sortedResponseTimes, numberOfRequests), min, max, medium, standardDeviation)
				results = results + (requestName -> result)
		}
		results
	}

}