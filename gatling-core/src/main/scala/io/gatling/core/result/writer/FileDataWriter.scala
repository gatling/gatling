/**
 * Copyright 2011-2013 eBusiness Information, Groupe Excilys (www.excilys.com)
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
package io.gatling.core.result.writer

import java.io.{ BufferedOutputStream, FileOutputStream, OutputStream }

import com.dongxiguo.fastring.Fastring.Implicits._
import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.result.message.{ ActionRecordType, GroupRecord, GroupRecordType, RequestRecord, RunRecord, RunRecordType, ScenarioRecord, ScenarioRecordType, ShortScenarioDescription }
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.util.StringHelper.eol

object FileDataWriter {

	val emptyField = " "

	val sanitizerPattern = """[\n\r\t]""".r

	/**
	 * Converts whitespace characters that would break the simulation log format into spaces.
	 */
	def sanitize(s: String): String = Option(s).map(s => sanitizerPattern.replaceAllIn(s, " ")).getOrElse("")

	implicit class RunRecordSerializer(val runRecord: RunRecord) extends AnyVal {

		def getBytes = {
			import runRecord._
			val description = if (runDescription.isEmpty) FileDataWriter.emptyField else runDescription
			val string = s"${RunRecordType.name}\t$timestamp\t$simulationId\t$description$eol"
			string.getBytes(configuration.core.encoding)
		}
	}

	implicit class ScenarioRecordSerializer(val scenarioRecord: ScenarioRecord) extends AnyVal {

		def getBytes = {
			import scenarioRecord._
			val string = s"${ScenarioRecordType.name}\t$scenarioName\t$userId\t${event.name}\t$executionDate$eol"
			string.getBytes(configuration.core.encoding)
		}
	}

	// fastring macro won't work inside a value class in 2.10
	object RequestRecordSerializer {

		def serialize(requestRecord: RequestRecord) = {
			import requestRecord._

			val message = requestMessage.getOrElse(emptyField)
			val serializedExtraInfo = extraInfo.map(info => fast"\t${sanitize(info.toString)}").mkFastring

			fast"${ActionRecordType.name}\t$scenarioName\t$userId\t$requestName\t$executionStartDate\t$requestSendingEndDate\t$responseReceivingStartDate\t$executionEndDate\t$requestStatus\t$message$serializedExtraInfo$eol"
		}
	}

	implicit class RequestRecordSerializer(val requestRecord: RequestRecord) extends AnyVal {

		def getBytes = {
			val string = RequestRecordSerializer.serialize(requestRecord).toString
			string.getBytes(configuration.core.encoding)
		}
	}

	implicit class GroupRecordRecordSerializer(val groupRecord: GroupRecord) extends AnyVal {

		def getBytes = {
			import groupRecord._
			val string = s"${GroupRecordType.name}\t$scenarioName\t$groupName\t$userId\t${event.name}\t$executionDate$eol"
			string.getBytes(configuration.core.encoding)
		}
	}
}

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter with Logging {

	import FileDataWriter._

	/**
	 * The OutputStreamWriter used to write to files
	 */
	private var os: OutputStream = _

	override def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {
		val simulationLog = simulationLogDirectory(runRecord.runId) / "simulation.log"
		os = new BufferedOutputStream(new FileOutputStream(simulationLog.toString))
		os.write(runRecord.getBytes)
	}

	override def onScenarioRecord(scenarioRecord: ScenarioRecord) {
		os.write(scenarioRecord.getBytes)
	}

	override def onGroupRecord(groupRecord: GroupRecord) {
		os.write(groupRecord.getBytes)
	}

	override def onRequestRecord(requestRecord: RequestRecord) {
		os.write(requestRecord.getBytes)
	}

	override def onFlushDataWriter {

		logger.info("Received flush order")

		withCloseable(os) { _.flush }
	}
}
