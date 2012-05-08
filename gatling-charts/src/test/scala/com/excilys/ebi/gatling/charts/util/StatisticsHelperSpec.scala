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
package com.excilys.ebi.gatling.charts.util

import scala.collection.mutable.ListBuffer

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ responseTimeStandardDeviation, averageTime }
import com.excilys.ebi.gatling.core.result.message.{ RequestStatus, RequestRecord }

@RunWith(classOf[JUnitRunner])
class StatisticsHelperSpec extends Specification {

	val emptyRequestData: Seq[RequestRecord] = new ListBuffer[RequestRecord]
	val testRequestRecords: Seq[RequestRecord] = createTestRequestRecords

	"empty request data: { }" should {

		"should return NO_PLOT_MAGIC_VALUE for averageTime(_.responseTime)" in {
			averageTime(_.responseTime)(emptyRequestData) must beEqualTo(StatisticsHelper.NO_PLOT_MAGIC_VALUE)
		}

		"return NO_PLOT_MAGIC_VALUE for responseTimeStandardDeviation" in {
			responseTimeStandardDeviation(emptyRequestData) must beEqualTo(StatisticsHelper.NO_PLOT_MAGIC_VALUE)
		}

	}

	"real request data: {2000, 4000, 4000, 4000, 5000, 5000, 7000, 9000}" should {
		"return 5000 for averageTime(_.responseTime)" in {
			averageTime(_.responseTime)(testRequestRecords) must beEqualTo(5000)
		}

		"return 2000 for responseTimeStandardDeviation" in {
			responseTimeStandardDeviation(testRequestRecords) must beEqualTo(2000)
		}

	}

	private def createTestRequestRecords(): Seq[RequestRecord] = {
		val requestRecords: ListBuffer[RequestRecord] = new ListBuffer[RequestRecord]
		// Wolfram Alpha reports the following statistics for {2000, 4000, 4000, 4000, 5000, 5000, 7000, 9000}:
		//   average (mean): 5000
		//   population stddev: 2000 - provided by Wikipedia & NeoOffice
		//   sample stddev: 2138
		//   median: 4500
		// http://www.wolframalpha.com/input/?i=2000%2C+4000%2C+4000%2C+4000%2C+5000%2C+5000%2C+7000%2C+9000
		requestRecords + createRequestRecord(1000, 3000, 3500, 5000)
		requestRecords + createRequestRecord(2000, 6000, 3500, 5000)
		requestRecords + createRequestRecord(3000, 7000, 3500, 5000)
		requestRecords + createRequestRecord(4000, 8000, 3500, 5000)
		requestRecords + createRequestRecord(5000, 10000, 3500, 5000)
		requestRecords + createRequestRecord(5000, 10000, 3500, 5000)
		requestRecords + createRequestRecord(7000, 14000, 3500, 5000)
		requestRecords + createRequestRecord(0, 9000, 3500, 5000)

		requestRecords
	}

	private def createRequestRecord(execStart: Long, execEnd: Long, sendEnd: Long, responseRecvStart: Long): RequestRecord = RequestRecord(
		"testScenario", 1, "Test Request", execStart, execEnd, sendEnd, responseRecvStart, RequestStatus.OK, "test request record")
}