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
package io.gatling.charts.result.reader

import scala.collection.mutable

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import io.gatling.core.ConfigurationConstants
import io.gatling.core.config.{ GatlingConfiguration, GatlingPropertiesBuilder }
import io.gatling.core.util.DateHelper.parseTimestampString

@RunWith(classOf[JUnitRunner])
class FileDataReaderSpec extends Specification {

	import ConfigurationConstants._

	// Tests must be executed sequentially to avoid configuration conflicts
	override def is = sequential ^ super.is

	val init = {
		val props = new GatlingPropertiesBuilder
		props.sourcesDirectory("src/test/resources")
		props.resultsDirectory("src/test/resources")

		GatlingConfiguration.setUp(props.build)
	}

	// FIXME re-enable with fresh and SIMPLE samples
	//	"When reading a single log file, FileDataReader" should {
	//
	//		val singleFileDataReader = new FileDataReader("run_single_node")
	//
	//		"be able to read a single file simulation" in {
	//			singleFileDataReader must not be null
	//		}
	//
	//		"find the two correct scenarios" in {
	//			singleFileDataReader.scenarioNames must beEqualTo(List("Scenario name", "Other Scenario Name"))
	//		}
	//
	//		"find the fifteen correct requests" in {
	//			val requestNames = List("Request request_1", "Request request_2", "Request request_3", "Request request_4", "Request request_5", "Request request_6", "Request request_7", "Request request_8", "Request request_9", "Request request_10")
	//			val otherRequestNames = List("Request other_request_1", "Request other_request_2", "Request other_request_3", "Request other_request_9", "Request other_request_10")
	//			singleFileDataReader.groupsAndRequests.collect { case (group, Some(request)) => RequestPath.path(request, group)} must haveTheSameElementsAs(requestNames ++ otherRequestNames)
	//		}
	//
	//		"have a correct run record" in {
	//			singleFileDataReader.runMessage must beEqualTo(RunMessage(parseTimestampString("20120607202804"), "run1", "interesting test run"))
	//		}
	//
	//	}
	//
	//	"When reading two log files coming from a multinode simulation, FileDataReader" should {
	//
	//		val multipleFilesDataReader = new FileDataReader("run_multiple_nodes")
	//
	//		"be able to read a multiple files simulation" in {
	//			multipleFilesDataReader must not be null
	//		}
	//
	//		"find the two correct scenarios" in {
	//			multipleFilesDataReader.scenarioNames must beEqualTo(List("Scenario name", "Other Scenario Name"))
	//		}
	//
	//		"find the fifteen correct requests" in {
	//			val requestNames = List("Request request_1", "Request request_2", "Request request_3", "Request request_4", "Request request_5", "Request request_6", "Request request_7", "Request request_8", "Request request_9", "Request request_10")
	//			val otherRequestNames = List("Request other_request_1", "Request other_request_2", "Request other_request_3", "Request other_request_9", "Request other_request_10")
	//			multipleFilesDataReader.groupsAndRequests.collect { case (group, Some(request)) => RequestPath.path(request, group)} must haveTheSameElementsAs(requestNames ++ otherRequestNames)
	//		}
	//
	//		//TODO - how to define correctly the runMessage method
	//		"have correct run records" in {
	//			multipleFilesDataReader.runMessage must not be null
	//		}
	//	}

	"When reading a single log file with known statistics, FileDataReder" should {
		val singleFileDataReader = new FileDataReader("run_single_node_with_known_stats")

		"return expected minResponseTime for correct request data" in {
			singleFileDataReader.requestGeneralStats().min must beEqualTo(2000L)
		}

		"return expected maxResponseTime for correct request data" in {
			singleFileDataReader.requestGeneralStats().max must beEqualTo(9000L)
		}

		"return expected responseTimeStandardDeviation for correct request data" in {
			singleFileDataReader.requestGeneralStats().stdDev must beEqualTo(2000L)
		}

		"return expected responseTimePercentile for the (0, 0.7) percentiles" in {
			val props = mutable.Map.empty[String, Any]
			props.put(CONF_CHARTING_INDICATORS_PERCENTILE1, 0)
			props.put(CONF_CHARTING_INDICATORS_PERCENTILE2, 70)
			props.put(CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val lowPercentilesFileDataReader = new FileDataReader("run_single_node_with_known_stats")
			lowPercentilesFileDataReader.requestGeneralStats().percentile1 must beEqualTo(2000L)
			lowPercentilesFileDataReader.requestGeneralStats().percentile2 must beEqualTo(5000L)
		}

		"return expected result for the (99.99, 100) percentiles" in {
			val props = mutable.Map.empty[String, Any]
			props.put(CONF_CHARTING_INDICATORS_PERCENTILE1, 99)
			props.put(CONF_CHARTING_INDICATORS_PERCENTILE2, 100)
			props.put(CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val highPercentilesFileDataReader = new FileDataReader("run_single_node_with_known_stats")
			highPercentilesFileDataReader.requestGeneralStats().percentile1 must beEqualTo(9000L)
			highPercentilesFileDataReader.requestGeneralStats().percentile2 must beEqualTo(9000L)
		}

		"indicate that all the request have their response time in between 0 and 100000" in {
			val props = mutable.Map.empty[String, Any]
			props.put(CONF_CHARTING_INDICATORS_LOWER_BOUND, 0)
			props.put(CONF_CHARTING_INDICATORS_HIGHER_BOUND, 100000)
			props.put(CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val fileDataReader = new FileDataReader("run_single_node_with_known_stats")
			fileDataReader.numberOfRequestInResponseTimeRange().map(_._2) must beEqualTo(List(0L, 8L, 0L, 0L))
		}

		"indicate that 1 request had a response time below 2500ms" in {
			val props = mutable.Map.empty[String, Any]
			props.put(CONF_CHARTING_INDICATORS_LOWER_BOUND, 2500)
			props.put(CONF_CHARTING_INDICATORS_HIGHER_BOUND, 5000)
			props.put(CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val nRequestInResponseTimeRange = new FileDataReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange().map(_._2)
			nRequestInResponseTimeRange(0) must beEqualTo(1L)
		}

		"indicate that 5 request had a response time in between 2500ms and 5000ms" in {
			val props = mutable.Map.empty[String, Any]
			props.put(CONF_CHARTING_INDICATORS_LOWER_BOUND, 2500)
			props.put(CONF_CHARTING_INDICATORS_HIGHER_BOUND, 5000)
			props.put(CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val nRequestInResponseTimeRange = new FileDataReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange().map(_._2)
			nRequestInResponseTimeRange(1) must beEqualTo(5L)
		}

		"indicate that 2 request had a response time above 5000ms" in {
			val props = mutable.Map.empty[String, Any]
			props.put(CONF_CHARTING_INDICATORS_LOWER_BOUND, 2500)
			props.put(CONF_CHARTING_INDICATORS_HIGHER_BOUND, 5000)
			props.put(CONF_CORE_DIRECTORY_SIMULATIONS, "src/test/resources")
			props.put(CONF_CORE_DIRECTORY_RESULTS, "src/test/resources")
			GatlingConfiguration.setUp(props)
			val nRequestInResponseTimeRange = new FileDataReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange().map(_._2)
			nRequestInResponseTimeRange(2) must beEqualTo(2L)
		}
	}
}
