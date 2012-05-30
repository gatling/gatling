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

import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ responseTimeStandardDeviation, responseTimePercentile, numberOfRequestInResponseTimeRange, minResponseTime, maxResponseTime, meanResponseTime, meanLatency, NO_PLOT_MAGIC_VALUE }
import com.excilys.ebi.gatling.core.result.message.{ RequestStatus, RequestRecord }

@RunWith(classOf[JUnitRunner])
class StatisticsHelperSpec extends Specification {

	// Wolfram Alpha reports the following statistics for {2000, 4000, 4000, 4000, 5000, 5000, 7000, 9000}:
	//   mean: 5000
	//   population stddev: 2000 - provided by Wikipedia & NeoOffice
	//   sample stddev: 2138
	//   median: 4500
	// http://www.wolframalpha.com/input/?i=2000%2C+4000%2C+4000%2C+4000%2C+5000%2C+5000%2C+7000%2C+9000
	val testRequestRecords = List(
		createRequestRecord(1000, 3000, 3500, 5000),
		createRequestRecord(1000, 10000, 3500, 5000),
		createRequestRecord(2000, 6000, 3500, 5000),
		createRequestRecord(3000, 7000, 3500, 5000),
		createRequestRecord(4000, 8000, 3500, 5000),
		createRequestRecord(5000, 10000, 3500, 5000),
		createRequestRecord(5000, 10000, 3500, 5000),
		createRequestRecord(7000, 14000, 3500, 5000))
	val knownAverageResponseTime = 5000

	private def createRequestRecord(execStart: Long, execEnd: Long, sendEnd: Long, responseRecvStart: Long): RequestRecord = RequestRecord(
		"testScenario", 1, "Test Request", execStart, execEnd, sendEnd, responseRecvStart, RequestStatus.OK, "test request record")

	"minResponseTime" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			minResponseTime(Nil) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			minResponseTime(testRequestRecords) must beEqualTo(2000)
		}
	}

	"maxResponseTime" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			maxResponseTime(Nil) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			maxResponseTime(testRequestRecords) must beEqualTo(9000)
		}
	}

	"meanResponseTime" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			meanResponseTime(Nil) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			meanResponseTime(testRequestRecords) must beEqualTo(knownAverageResponseTime)
		}
	}

	"meanLatency" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			meanLatency(Nil) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			meanLatency(testRequestRecords) must beEqualTo(1500)
		}
	}

	"responseTimeStandardDeviation" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			responseTimeStandardDeviation(Nil) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			responseTimeStandardDeviation(testRequestRecords) must beEqualTo(2000)
		}
	}

	"responseTimePercentile" should {
		"return expected result for the 0 percentile" in {
			responseTimePercentile(testRequestRecords.sortBy(_.responseTime), 0) must beEqualTo(2000)
		}
		"return expected result for the 70 percentile" in {
			responseTimePercentile(testRequestRecords.sortBy(_.responseTime), 0.70) must beEqualTo(5000)
		}

		"return expected result for the 95 percentile" in {
			responseTimePercentile(testRequestRecords.sortBy(_.responseTime), 0.95) must beEqualTo(9000)
		}

		"return expected result for the 99.99 percentile" in {
			responseTimePercentile(testRequestRecords.sortBy(_.responseTime), 0.9999) must beEqualTo(9000)
		}

		"return expected result for the 100 percentile" in {
			responseTimePercentile(testRequestRecords.sortBy(_.responseTime), 1) must throwA[IndexOutOfBoundsException]
		}
	}

	"numberOfRequestInResponseTimeRange" should {
		"indicate that all the request have their response time in between 0 and 100000" in {
			numberOfRequestInResponseTimeRange(testRequestRecords, 0, 100000).map(_._2) must beEqualTo(List(0, 8, 0, 0))
		}

		val nRequestInResponseTimeRange = numberOfRequestInResponseTimeRange(testRequestRecords, 2500, 5000).map(_._2)

		"indicate that 1 request had a response time below 2500ms" in {
			nRequestInResponseTimeRange(0) must beEqualTo(1)
		}
		"indicate that 5 request had a response time in between 2500ms and 5000ms" in {
			nRequestInResponseTimeRange(1) must beEqualTo(5)
		}

		"indicate that 2 request had a response time above 5000ms" in {
			nRequestInResponseTimeRange(2) must beEqualTo(2)
		}
	}

}