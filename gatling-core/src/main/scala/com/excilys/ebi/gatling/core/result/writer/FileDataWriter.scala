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
package com.excilys.ebi.gatling.core.result.writer

import java.io.FileOutputStream

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogDirectory
import com.excilys.ebi.gatling.core.result.message.{ RequestRecord, RunRecord, ScenarioRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.message.GroupRecord
import com.excilys.ebi.gatling.core.result.message.RecordType.{ ACTION, GROUP, RUN, SCENARIO }
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

import grizzled.slf4j.Logging

object FileDataWriter {

	val emptyField = " "
	val sanitizerPattern = """[\n\r\t]""".r
	def sanitize(input: String): String = Option(sanitizerPattern.replaceAllIn(input, " ")).getOrElse("")

	def serialize(requestRecord: RequestRecord) = {
		val sb = new StringBuilder().append(ACTION).append(TABULATION_SEPARATOR)
			.append(requestRecord.scenarioName).append(TABULATION_SEPARATOR)
			.append(requestRecord.userId.toString).append(TABULATION_SEPARATOR)
			.append(requestRecord.requestName).append(TABULATION_SEPARATOR)
			.append(requestRecord.executionStartDate.toString).append(TABULATION_SEPARATOR)
			.append(requestRecord.requestSendingEndDate.toString).append(TABULATION_SEPARATOR)
			.append(requestRecord.responseReceivingStartDate.toString).append(TABULATION_SEPARATOR)
			.append(requestRecord.executionEndDate.toString).append(TABULATION_SEPARATOR)
			.append(requestRecord.requestStatus.toString).append(TABULATION_SEPARATOR)
			.append(requestRecord.requestMessage.getOrElse(emptyField))

		requestRecord.extraInfo.foreach((info: String) => {
			sb.append(TABULATION_SEPARATOR).append(sanitize(info))
		})

		sb.append(END_OF_LINE)
			.toString.getBytes(configuration.core.encoding)
	}

	def serialize(runRecord: RunRecord) =
		new StringBuilder().append(RUN).append(TABULATION_SEPARATOR)
			.append(runRecord.timestamp).append(TABULATION_SEPARATOR)
			.append(runRecord.simulationId).append(TABULATION_SEPARATOR)
			// hack for being able to deserialize in FileDataReader
			.append(if (runRecord.runDescription.isEmpty) emptyField else runRecord.runDescription)
			.append(END_OF_LINE)
			.toString.getBytes(configuration.core.encoding)

	def serialize(scenarioRecord: ScenarioRecord) = {
		new StringBuilder().append(SCENARIO).append(TABULATION_SEPARATOR)
			.append(scenarioRecord.scenarioName).append(TABULATION_SEPARATOR)
			.append(scenarioRecord.userId.toString).append(TABULATION_SEPARATOR)
			.append(scenarioRecord.event).append(TABULATION_SEPARATOR)
			.append(scenarioRecord.executionDate.toString)
			.append(END_OF_LINE)
			.toString.getBytes(configuration.core.encoding)
	}

	def serialize(groupRecord: GroupRecord) =
		new StringBuilder().append(GROUP).append(TABULATION_SEPARATOR)
			.append(groupRecord.scenarioName).append(TABULATION_SEPARATOR)
			.append(groupRecord.groupName).append(TABULATION_SEPARATOR)
			.append(groupRecord.userId.toString).append(TABULATION_SEPARATOR)
			.append(groupRecord.event).append(TABULATION_SEPARATOR)
			.append(groupRecord.executionDate.toString)
			.append(END_OF_LINE)
			.toString.getBytes(configuration.core.encoding)
}

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter with Logging {

	import FileDataWriter._

	private val bufferSize = configuration.data.file.bufferSize
	private var bufferPosition = 0
	private val buffer = new Array[Byte](bufferSize)
	private var os: FileOutputStream = _

	private def flush {
		os.write(buffer, 0, bufferPosition)
		bufferPosition = 0
	}

	private def write(bytes: Array[Byte]) {
		if (bytes.length + bufferPosition > bufferSize) {
			flush
		}

		if (bytes.length > bufferSize) {
			// can't write in buffer
			warn("Buffer size " + bufferSize + " is not sufficient for message of size " + bytes.length)
			os.write(bytes)

		} else {
			System.arraycopy(bytes, 0, buffer, bufferPosition, bytes.length)
			bufferPosition += bytes.length
		}
	}

	override def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {
		val simulationLog = simulationLogDirectory(runRecord.runId) / "simulation.log"
		os = new FileOutputStream(simulationLog.toString)
		write(serialize(runRecord))
	}

	override def onScenarioRecord(scenarioRecord: ScenarioRecord) {
		write(serialize(scenarioRecord))
	}

	override def onGroupRecord(groupRecord: GroupRecord) {
		write(serialize(groupRecord))
	}

	override def onRequestRecord(requestRecord: RequestRecord) {
		write(serialize(requestRecord))
	}

	override def onFlushDataWriter {

		info("Received flush order")

		use(os) { _ => flush }
	}
}
