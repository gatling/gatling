/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.charts.result.reader

import scala.collection.mutable

import io.gatling.BaseSpec
import io.gatling.charts.stats.LogFileReader
import io.gatling.core.ConfigKeys._
import io.gatling.core.config.{ GatlingConfiguration, GatlingPropertiesBuilder }

class LogFileReaderSpec extends BaseSpec {

  val props = new GatlingPropertiesBuilder
  props.resultsDirectory("src/test/resources")

  implicit val configuration = GatlingConfiguration.loadForTest(props.build)

  // FIXME re-enable with fresh and SIMPLE samples
  //	"When reading a single log file, LogFileReader" should {
  //
  //		val singleLogFileReader = new LogFileReader("run_single_node")
  //
  //		"be able to read a single file simulation" in {
  //			singleLogFileReader must not be null
  //		}
  //
  //		"find the two correct scenarios" in {
  //			singleLogFileReader.scenarioNames must beEqualTo(List("Scenario name", "Other Scenario Name"))
  //		}
  //
  //		"find the fifteen correct requests" in {
  //			val requestNames = List("Request request_1", "Request request_2", "Request request_3", "Request request_4", "Request request_5", "Request request_6", "Request request_7", "Request request_8", "Request request_9", "Request request_10")
  //			val otherRequestNames = List("Request other_request_1", "Request other_request_2", "Request other_request_3", "Request other_request_9", "Request other_request_10")
  //			singleLogFileReader.groupsAndRequests.collect { case (group, Some(request)) => RequestPath.path(request, group)} must haveTheSameElementsAs(requestNames ++ otherRequestNames)
  //		}
  //
  //		"have a correct run record" in {
  //			singleLogFileReader.runMessage must beEqualTo(RunMessage(parseTimestampString("20120607202804"), "run1", "interesting test run"))
  //		}
  //
  //	}
  //
  //	"When reading two log files coming from a multinode simulation, LogFileReader" should {
  //
  //		val multipleFilesDataReader = new LogFileReader("run_multiple_nodes")
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

  val singleLogFileReader = new LogFileReader("run_single_node_with_known_stats")
  "When reading a single log file with known statistics, FileDataReder" should "return expected minResponseTime for correct request data" in {
    singleLogFileReader.requestGeneralStats().min shouldBe 2000
  }

  it should "return expected maxResponseTime for correct request data" in {
    singleLogFileReader.requestGeneralStats().max shouldBe 9000
  }

  it should "return expected responseTimeStandardDeviation for correct request data" in {
    val computedValue = singleLogFileReader.requestGeneralStats().stdDev
    val expectedValue = 2138
    val error = (computedValue.toDouble - expectedValue) / expectedValue

    error shouldBe <=(0.06)
  }

  it should "return expected responseTimePercentile for the (0, 0.7) percentiles" in {
    val props = mutable.Map.empty[String, Any]
    props.put(charting.indicators.Percentile1, 0)
    props.put(charting.indicators.Percentile2, 70)
    props.put(core.directory.Simulations, "src/test/resources")
    props.put(core.directory.Results, "src/test/resources")
    implicit val configuration = GatlingConfiguration.loadForTest(props)
    val lowPercentilesLogFileReader = new LogFileReader("run_single_node_with_known_stats")
    lowPercentilesLogFileReader.requestGeneralStats().percentile(configuration.charting.indicators.percentile1) shouldBe 2000
    lowPercentilesLogFileReader.requestGeneralStats().percentile(configuration.charting.indicators.percentile2) shouldBe 5000
  }

  it should "return expected result for the (99.99, 100) percentiles" in {
    val props = mutable.Map.empty[String, Any]
    props.put(charting.indicators.Percentile1, 99)
    props.put(charting.indicators.Percentile2, 100)
    props.put(core.directory.Simulations, "src/test/resources")
    props.put(core.directory.Results, "src/test/resources")
    implicit val configuration = GatlingConfiguration.loadForTest(props)
    val highPercentilesLogFileReader = new LogFileReader("run_single_node_with_known_stats")
    highPercentilesLogFileReader.requestGeneralStats().percentile(configuration.charting.indicators.percentile1) shouldBe 8860
    highPercentilesLogFileReader.requestGeneralStats().percentile(configuration.charting.indicators.percentile2) shouldBe 9000
  }

  it should "indicate that all the request have their response time in between 0 and 100000" in {
    val props = mutable.Map.empty[String, Any]
    props.put(charting.indicators.LowerBound, 0)
    props.put(charting.indicators.HigherBound, 100000)
    props.put(core.directory.Simulations, "src/test/resources")
    props.put(core.directory.Results, "src/test/resources")
    implicit val configuration = GatlingConfiguration.loadForTest(props)
    val fileDataReader = new LogFileReader("run_single_node_with_known_stats")
    fileDataReader.numberOfRequestInResponseTimeRange(None, None).map(_._2) shouldBe List(0, 8, 0, 0)
  }

  it should "indicate that 1 request had a response time below 2500ms" in {
    val props = mutable.Map.empty[String, Any]
    props.put(charting.indicators.LowerBound, 2500)
    props.put(charting.indicators.HigherBound, 5000)
    props.put(core.directory.Simulations, "src/test/resources")
    props.put(core.directory.Results, "src/test/resources")
    implicit val configuration = GatlingConfiguration.loadForTest(props)
    val nRequestInResponseTimeRange = new LogFileReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange(None, None).map(_._2)
    nRequestInResponseTimeRange.head shouldBe 1
  }

  it should "indicate that 5 request had a response time in between 2500ms and 5000ms" in {
    val props = mutable.Map.empty[String, Any]
    props.put(charting.indicators.LowerBound, 2500)
    props.put(charting.indicators.HigherBound, 5000)
    props.put(core.directory.Simulations, "src/test/resources")
    props.put(core.directory.Results, "src/test/resources")
    implicit val configuration = GatlingConfiguration.loadForTest(props)
    val nRequestInResponseTimeRange = new LogFileReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange(None, None).map(_._2)
    nRequestInResponseTimeRange(1) shouldBe 5
  }

  it should "indicate that 2 request had a response time above 5000ms" in {
    val props = mutable.Map.empty[String, Any]
    props.put(charting.indicators.LowerBound, 2500)
    props.put(charting.indicators.HigherBound, 5000)
    props.put(core.directory.Simulations, "src/test/resources")
    props.put(core.directory.Results, "src/test/resources")
    implicit val configuration = GatlingConfiguration.loadForTest(props)
    val nRequestInResponseTimeRange = new LogFileReader("run_single_node_with_known_stats").numberOfRequestInResponseTimeRange(None, None).map(_._2)
    nRequestInResponseTimeRange(2) shouldBe 2
  }
}
