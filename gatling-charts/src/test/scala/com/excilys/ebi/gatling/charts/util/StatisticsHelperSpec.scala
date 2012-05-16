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

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ responseTimeStandardDeviation, averageResponseTime, averageLatency }
import com.excilys.ebi.gatling.core.result.message.{ RequestStatus, RequestRecord }

@RunWith(classOf[JUnitRunner])
class StatisticsHelperSpec extends Specification {

	// Wolfram Alpha reports the following statistics for {2000, 4000, 4000, 4000, 5000, 5000, 7000, 9000}:
	//   average (mean): 5000
	//   population stddev: 2000 - provided by Wikipedia & NeoOffice
	//   sample stddev: 2138
	//   median: 4500
	// http://www.wolframalpha.com/input/?i=2000%2C+4000%2C+4000%2C+4000%2C+5000%2C+5000%2C+7000%2C+9000
	val testRequestRecords = List(
		createRequestRecord(1000, 3000, 3500, 5000),
		createRequestRecord(2000, 6000, 3500, 5000),
		createRequestRecord(3000, 7000, 3500, 5000),
		createRequestRecord(4000, 8000, 3500, 5000),
		createRequestRecord(5000, 10000, 3500, 5000),
		createRequestRecord(5000, 10000, 3500, 5000),
		createRequestRecord(7000, 14000, 3500, 5000),
		createRequestRecord(0, 9000, 3500, 5000))

	private def createRequestRecord(execStart: Long, execEnd: Long, sendEnd: Long, responseRecvStart: Long): RequestRecord = RequestRecord(
		"testScenario", 1, "Test Request", execStart, execEnd, sendEnd, responseRecvStart, RequestStatus.OK, "test request record")

	"averageResponseTime" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			averageResponseTime(Nil) must beEqualTo(StatisticsHelper.NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			averageResponseTime(testRequestRecords) must beEqualTo(5000)
		}
	}

	"averageLatency" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			averageLatency(Nil) must beEqualTo(StatisticsHelper.NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			averageLatency(testRequestRecords) must beEqualTo(1500)
		}
	}

	"responseTimeStandardDeviation" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			responseTimeStandardDeviation(Nil) must beEqualTo(StatisticsHelper.NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			responseTimeStandardDeviation(testRequestRecords) must beEqualTo(2000)
		}
	}
}