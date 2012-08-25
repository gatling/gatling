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
package com.excilys.ebi.gatling.charts.result.reader

import scala.tools.nsc.io.Path

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import com.excilys.ebi.gatling.core.config.{ GatlingConfiguration, GatlingOptions }
import com.excilys.ebi.gatling.core.result.message.{ RequestStatus, RunRecord }
import com.excilys.ebi.gatling.core.result.reader.ChartRequestRecord
import com.excilys.ebi.gatling.core.util.DateHelper.parseTimestampString

@RunWith(classOf[JUnitRunner])
class FileDataReaderSpec extends Specification {

	//The file data reader needs to know the encoding, use default conf.

	val conf = GatlingOptions(
		simulationSourcesDirectory = Some(Path(List("src", "test", "resources")).toDirectory),
		resultsDirectory = Some(Path(List("src", "test", "resources"))))

	GatlingConfiguration.setUp(conf)

	"When reading a single log file, FileDataReader" should {

		val singleFileDataReader = new FileDataReader("run_single_node")

		"be able to read a single file simulation" in {
			singleFileDataReader must not be null
		}

		"find the two correct scenarios" in {
			singleFileDataReader.scenarioNames must beEqualTo(List("Scenario name", "Other Scenario Name"))
		}

		"find the two correct scenarios" in {
			val requestNames = List("Request request_1", "Request request_2", "Request request_3", "Request request_4", "Request request_5", "Request request_6", "Request request_7", "Request request_8", "Request request_9", "Request request_10")
			val otherRequestNames = List("Request other_request_1", "Request other_request_2", "Request other_request_3", "Request other_request_9", "Request other_request_10")
			singleFileDataReader.requestNames must haveTheSameElementsAs(requestNames ++ otherRequestNames)
		}

		"have a correct run record" in {
			singleFileDataReader.runRecord must beEqualTo(RunRecord(parseTimestampString("20120607202804"), "run1", "interesting test run"))
		}

		"have read all the request records" in {
			singleFileDataReader.requestRecords must have size 353
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

		"find the two correct scenarios" in {
			val requestNames = List("Request request_1", "Request request_2", "Request request_3", "Request request_4", "Request request_5", "Request request_6", "Request request_7", "Request request_8", "Request request_9", "Request request_10")
			val otherRequestNames = List("Request other_request_1", "Request other_request_2", "Request other_request_3", "Request other_request_9", "Request other_request_10")
			multipleFilesDataReader.requestNames must haveTheSameElementsAs(requestNames ++ otherRequestNames)
		}

		"have read all the request records" in {
			multipleFilesDataReader.requestRecords must have size 706
		}

		"have a record from the first node simulation log" in {
			val record = ChartRequestRecord("Scenario name", 2, "Request request_2", 1339408458535L, 1339408458772L, 1339408458537L, 1339408458772L, RequestStatus.withName("OK"))
			multipleFilesDataReader.requestRecords must contain(record)
		}

		"have a record from the second node simulation log" in {
			val record = ChartRequestRecord("Scenario name", 1, "Request request_1", 1339407593167L, 1339407593907L, 1339407593692L, 1339407593904L, RequestStatus.withName("OK"))
			multipleFilesDataReader.requestRecords must contain(record)
		}

		"have correct run records" in {
			val node1RunRecord = RunRecord(parseTimestampString("20120611115415"), "run", "node1")
			val node2RunRecord = RunRecord(parseTimestampString("20120611113951"), "run", "node2")
			multipleFilesDataReader.allRunRecords must haveTheSameElementsAs(List(node1RunRecord, node2RunRecord))
		}

		//TODO - how to define correctly the runRecord method
		"have correct run records" in {
			multipleFilesDataReader.runRecord must not be null
		}
	}
}