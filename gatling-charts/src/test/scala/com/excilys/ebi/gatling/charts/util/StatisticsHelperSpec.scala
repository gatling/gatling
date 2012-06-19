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

import com.excilys.ebi.gatling.charts.util.StatisticsHelper.{ responseTimeStandardDeviation, percentiles, numberOfRequestInResponseTimeRange, minResponseTime, meanResponseTime, meanLatency, maxResponseTime, NO_PLOT_MAGIC_VALUE }
import com.excilys.ebi.gatling.core.result.message.RequestStatus
import com.excilys.ebi.gatling.core.result.reader.ChartRequestRecord

@RunWith(classOf[JUnitRunner])
class StatisticsHelperSpec extends Specification {

	// Wolfram Alpha reports the following statistics for {2000, 4000, 4000, 4000, 5000, 5000, 7000, 9000}:
	//   mean: 5000
	//   population stddev: 2000 - provided by Wikipedia & NeoOffice
	//   sample stddev: 2138
	//   median: 4500
	// http://www.wolframalpha.com/input/?i=2000%2C+4000%2C+4000%2C+4000%2C+5000%2C+5000%2C+7000%2C+9000
	val testChartRequestRecords = List(
		createChartRequestRecord(1000, 3000, 3500, 5000),
		createChartRequestRecord(1000, 10000, 3500, 5000),
		createChartRequestRecord(2000, 6000, 3500, 5000),
		createChartRequestRecord(3000, 7000, 3500, 5000),
		createChartRequestRecord(4000, 8000, 3500, 5000),
		createChartRequestRecord(5000, 10000, 3500, 5000),
		createChartRequestRecord(5000, 10000, 3500, 5000),
		createChartRequestRecord(7000, 14000, 3500, 5000))
	val knownAverageResponseTime = 5000

	private def createChartRequestRecord(execStart: Long, execEnd: Long, sendEnd: Long, responseRecvStart: Long): ChartRequestRecord =
		ChartRequestRecord("testScenario", 1, "Test Request", execStart, execEnd, sendEnd, responseRecvStart, RequestStatus.OK)

	"minResponseTime" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			minResponseTime(Nil, None, None) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			minResponseTime(testChartRequestRecords, None, None) must beEqualTo(2000)
		}
	}

	"maxResponseTime" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			maxResponseTime(Nil, None, None) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			maxResponseTime(testChartRequestRecords, None, None) must beEqualTo(9000)
		}
	}

	"responseTimeStandardDeviation" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			responseTimeStandardDeviation(Nil) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			responseTimeStandardDeviation(testChartRequestRecords) must beEqualTo(2000)
		}
	}

	"responseTimePercentile" should {
		"return expected result for the (0, 0.7) percentiles" in {
			percentiles(testChartRequestRecords.sortBy(_.responseTime), 0, 0.7, None, None) must beEqualTo((2000, 7000))
		}

		"return expected result for the (99.99, 100) percentiles" in {
			percentiles(testChartRequestRecords.sortBy(_.responseTime), 0.9999, 1, None, None) must beEqualTo(9000, 9000)
		}
	}

	"numberOfRequestInResponseTimeRange" should {
		"indicate that all the request have their response time in between 0 and 100000" in {
			numberOfRequestInResponseTimeRange(testChartRequestRecords, 0, 100000, None).map(_._2) must beEqualTo(List(0, 8, 0, 0))
		}

		val nRequestInResponseTimeRange = numberOfRequestInResponseTimeRange(testChartRequestRecords, 2500, 5000, None).map(_._2)

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