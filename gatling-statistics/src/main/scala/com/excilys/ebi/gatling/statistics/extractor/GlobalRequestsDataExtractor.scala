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

import com.excilys.ebi.gatling.core.result.message.ResultStatus
import com.excilys.ebi.gatling.core.result.message.ResultStatus._
import com.excilys.ebi.gatling.core.util.PathHelper._

import scala.collection.mutable.HashMap

import java.lang.String

class GlobalRequestsDataExtractor extends DataExtractor[List[(String, (Double, Double, Double))]] {

	val failureRequestData: HashMap[String, Double] = new HashMap[String, Double]
	val successRequestData: HashMap[String, Double] = new HashMap[String, Double]
	val allRequestData: HashMap[String, Double] = new HashMap[String, Double]

	def onRow(runOn: String, scenarioName: String, userId: String, actionName: String, executionStartDate: String, executionDuration: String, resultStatus: String, resultMessage: String, groups: List[String]) {
		if (actionName startsWith "Request") {
			def inc = incrementInMap(executionStartDate)_

			try {
				ResultStatus.withName(resultStatus) match {
					case OK =>
						inc(successRequestData)
						inc(allRequestData)
					case KO =>
						inc(failureRequestData)
						inc(allRequestData)
				}
			} catch {
				case e => sys.error("Input file not well formated")
			}
		}
	}

	def getResults: List[(String, (Double, Double, Double))] = {

		var data: List[(String, (Double, Double, Double))] = Nil

		allRequestData.foreach {
			case (date, nbRequests) =>
				def get = getInMap(date)_
				data = (date, (nbRequests, get(successRequestData), get(failureRequestData))) :: data
		}

		data
	}

	private def getInMap(date: String)(map: HashMap[String, Double]): Double = {
		map.get(date).getOrElse(0)
	}

	private def incrementInMap(executionStartDate: String)(map: HashMap[String, Double]) = {
		map(executionStartDate) =
			if (map.contains(executionStartDate))
				map(executionStartDate) + 1
			else
				1
	}
}