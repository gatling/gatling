/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
package com.excilys.ebi.gatling.charts.result.reader

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.core.ConfigurationConstants
import com.excilys.ebi.gatling.core.config.{ GatlingConfiguration, GatlingPropertiesBuilder }
import com.excilys.ebi.gatling.core.result.RequestPath
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString

import java.util

@RunWith(classOf[JUnitRunner])
class FileDataReaderSpec extends Specification {

	// Tests must be executed sequentially to avoid configuration conflicts
	override def is = sequential ^ super.is

	val LOWER_BOUNDS = "gatling.charting.indicators.lowerBound"
	val HIGHER_BOUNDS = "gatling.charting.indicators.higherBound"
	val PERCENTILE_1 = "gatling.charting.indicators.percentile1"
	val PERCENTILE_2 = "gatling.charting.indicators.percentile2"

	val init = {
		val props = new GatlingPropertiesBuilder
		props.sourcesDirectory("src/test/resources")
		props.resultsDirectory("src/test/resources")

		GatlingConfiguration.setUp(props.build)
	}

	"When reading a single log file, FileDataReader" should {

		val singleFileDataReader = new FileDataReader("run_single_node")

		"be able to read a single file simulation" in {
			singleFileDataReader must not be null
		}

		"find the two correct scenarios" in {
			singleFileDataReader.scenarioNames must beEqualTo(List("Scenario name", "Other Scenario Name"))
		}

		"find the fifteen correct requests" in {
			val requestNames = List("Request request_1", "Request request_2", "Request request_3", "Request request_4", "Request request_5", "Request request_6", "Request request_7", "Request request_8", "Request request_9", "Request request_10")
			val otherRequestNames = List("Request other_request_1", "Request other_request_2", "Request other_request_3", "Request other_request_9", "Request other_request_10")
			singleFileDataReader.groupsAndRequests.collect { case (group, Some(request)) => RequestPath.path(request, group) } must haveTheSameElementsAs(requestNames ++ otherRequestNames)
		}

		"have a correct run record" in {
			singleFileDataReader.runRecord must beEqualTo(RunRecord(parseTimestampString("20120607202804"), "run1", "interesting test run"))
		}

	}

	"When reading two log files coming from a multinode simulation, FileDataReader" should {

		val multipleFilesDataReader = new FileDataReader("run_multiple_nodes")

		"be able to read a multiple files simulation" in {
			multipleFilesDataReader must not be null
		}

		"find the two correct scenarios" in {
			multipleFilesDataReader.scenarioNames must beEqualTo(List("Scenario name", "Other Scenario Name"))
		}

		"find the fifteen correct requests" in {
			val requestNames = List("Request request_1", "Request request_2", "Request request_3", "Request request_4", "Request request_5", "Request request_6", "Request request_7", "Request request_8", "Request request_9", "Request request_10")
			val otherRequestNames = List("Request other_request_1", "Request other_request_2", "Request other_request_3", "Request other_request_9", "Request other_request_10")
			multipleFilesDataReader.groupsAndRequests.collect { case (group, Some(request)) => RequestPath.path(request, group) } must haveTheSameElementsAs(requestNames ++ otherRequestNames)
		}

		//TODO - how to define correctly the runRecord method
		"have correct run records" in {
			multipleFilesDataReader.runRecord must not be null
		}
	}

	"When reading a single log file with known statistics, FileDataReder" should {
		val singleFileDataReader = new FileDataReader("run_single_node_with_known_stats")

		"return expected minResponseTime for correct request data" in {
			singleFileDataReader.generalStats().min must beEqualTo(2000L)
		}

		"return expected maxResponseTime for correct request data" in {
			singleFileDataReader.generalStats().max must beEqualTo(9000L)
		}

		"return expected responseTimeStandardDeviation for correct request data" in {
			singleFileDataReader.generalStats().stdDev must beEqualTo(2000L)
		}

		"return expected responseTimePercentile for the (0, 0.7) percentiles" in {
			val props = new util.HashMap[String, Any]()
			props.put(PERCENTILE_1, 0)
			props.put(PERCENTILE_2, 70)
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val lowPercentilesFileDataReader = new FileDataReader("run_single_node_with_known_stats")
			lowPercentilesFileDataReader.generalStats().percentile1 must beEqualTo(2000L)
			lowPercentilesFileDataReader.generalStats().percentile2 must beEqualTo(5000L)
		}

		"return expected result for the (99.99, 100) percentiles" in {
			val props = new util.HashMap[String, Any]()
			props.put(PERCENTILE_1, 99)
			props.put(PERCENTILE_2, 100)
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val highPercentilesFileDataReader = new FileDataReader("run_single_node_with_known_stats")
			highPercentilesFileDataReader.generalStats().percentile1 must beEqualTo(9000L)
			highPercentilesFileDataReader.generalStats().percentile2 must beEqualTo(9000L)
		}

		"indicate that all the request have their response time in between 0 and 100000" in {
			val props = new util.HashMap[String, Any]()
			props.put(LOWER_BOUNDS, 0)
			props.put(HIGHER_BOUNDS, 100000)
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val fileDataReader = new FileDataReader("run_single_node_with_known_stats")
			fileDataReader.numberOfRequestInResponseTimeRange().map(_._2) must beEqualTo(List(0L, 8L, 0L, 0L))
		}

		"indicate that 1 request had a response time below 2500ms" in {
			val props = new util.HashMap[String, Any]()
			props.put(LOWER_BOUNDS, 2500)
			props.put(HIGHER_BOUNDS, 5000)
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val nRequestInResponseTimeRange = new FileDataReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange().map(_._2)
			nRequestInResponseTimeRange(0) must beEqualTo(1L)
		}

		"indicate that 5 request had a response time in between 2500ms and 5000ms" in {
			val props = new util.HashMap[String, Any]()
			props.put(LOWER_BOUNDS, 2500)
			props.put(HIGHER_BOUNDS, 5000)
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val nRequestInResponseTimeRange = new FileDataReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange().map(_._2)
			nRequestInResponseTimeRange(1) must beEqualTo(5L)
		}

		"indicate that 2 request had a response time above 5000ms" in {
			val props = new util.HashMap[String, Any]()
			props.put(LOWER_BOUNDS, 2500)
			props.put(HIGHER_BOUNDS, 5000)
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(ConfigurationConstants.CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val nRequestInResponseTimeRange = new FileDataReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange().map(_._2)
			nRequestInResponseTimeRange(2) must beEqualTo(2L)
		}
	}
}
