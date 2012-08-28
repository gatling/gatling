/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.excilys.ebi.gatling.charts.result.reader.stats

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.charts.result.reader.stats.StatsResultsHelper._
import com.excilys.ebi.gatling.charts.result.reader.util.ResultBufferType._
import com.excilys.ebi.gatling.core.result.message.RequestStatus._

@RunWith(classOf[JUnitRunner])
class StatsResultsHelperSpec extends Specification {
	val testStatsResults = new StatsResults
	testStatsResults.getGeneralStatsBuffer(BY_STATUS) += new GeneralStatsRecord(12L, 589L, 8L, 58L, 45L, 14L, 15.2, Some(OK), None)
	testStatsResults.getResponseTimeDistributionBuffer(BY_STATUS) +=(new ResponseTimeDistributionRecord(2400L, 1L, Some(OK), None), new ResponseTimeDistributionRecord(3400L, 5L, Some(OK), None), new ResponseTimeDistributionRecord(7400L, 2L, Some(OK), None))

	"minResponseTime" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			getMinResponseTime(testStatsResults, None, None) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			getMinResponseTime(testStatsResults, Some(OK), None) must beEqualTo(12L)
		}
	}

	"maxResponseTime" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			getMaxResponseTime(testStatsResults, None, None) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			getMaxResponseTime(testStatsResults, Some(OK), None) must beEqualTo(589L)
		}
	}

	"responseTimeStandardDeviation" should {

		"return NO_PLOT_MAGIC_VALUE for empty request data" in {
			getResponseTimeStandardDeviation(testStatsResults, None, None) must beEqualTo(NO_PLOT_MAGIC_VALUE)
		}

		"return expected result for correct request data" in {
			getResponseTimeStandardDeviation(testStatsResults, Some(OK), None) must beEqualTo(15)
		}
	}

	"responseTimePercentile" should {
		"return expected result for the (0, 0.7) percentiles" in {
			getPercentiles(testStatsResults, 0, 0.7, Some(OK), None) must beEqualTo((2400L, 3400L))
		}

		"return expected result for the (99.99, 100) percentiles" in {
			getPercentiles(testStatsResults, 0.9999, 1., Some(OK), None) must beEqualTo(7400L, 7400L)
		}
	}

	"numberOfRequestInResponseTimeRange" should {
		"indicate that all the request have their response time in between 0 and 100000" in {
			getNumberOfRequestInResponseTimeRange(testStatsResults, 0, 100000, None).map(_._2) must beEqualTo(List(0, 8, 0, 0))
		}

		val nRequestInResponseTimeRange = getNumberOfRequestInResponseTimeRange(testStatsResults, 2500, 5000, None).map(_._2)

		"indicate that 1 request had a response time below 2500ms" in {
			nRequestInResponseTimeRange(0) must beEqualTo(1L)
		}
		"indicate that 5 request had a response time in between 2500ms and 5000ms" in {
			nRequestInResponseTimeRange(1) must beEqualTo(5L)
		}

		"indicate that 2 request had a response time above 5000ms" in {
			nRequestInResponseTimeRange(2) must beEqualTo(2L)
		}
	}

}