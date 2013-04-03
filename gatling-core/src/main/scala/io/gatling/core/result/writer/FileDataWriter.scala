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

import java.io.{ BufferedOutputStream, FileOutputStream, OutputStreamWriter }

import com.typesafe.scalalogging.slf4j.Logging

import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.config.GatlingFiles.simulationLogDirectory
import io.gatling.core.result.message.{ ActionRecordType, GroupRecord, GroupRecordType, RequestRecord, RunRecord, RunRecordType, ScenarioRecord, ScenarioRecordType, ShortScenarioDescription }
import io.gatling.core.util.FileHelper.tabulationSeparator
import io.gatling.core.util.IOHelper.withCloseable
import io.gatling.core.util.StringHelper.eol

object FileDataWriter {

	val emptyField = " "

	val sanitizerPattern = """[\n\r\t]""".r

	/**
	 * Converts whitespace characters that would break the simulation log format into spaces.
	 */
	def sanitize(s: String): String = Option(s).map(s => sanitizerPattern.replaceAllIn(s, " ")).getOrElse("")

	implicit class DataWriterMessageAppendable[T <: Appendable](val appendable: T) extends AnyVal {

		def append(scenarioRecord: ScenarioRecord): T = {
			appendable.append(ScenarioRecordType.name).append(tabulationSeparator)
				.append(scenarioRecord.scenarioName).append(tabulationSeparator)
				.append(scenarioRecord.userId.toString).append(tabulationSeparator)
				.append(scenarioRecord.event.name).append(tabulationSeparator)
				.append(scenarioRecord.executionDate.toString).append(eol)
			appendable
		}

		def append(groupRecord: GroupRecord): T = {
			appendable.append(GroupRecordType.name).append(tabulationSeparator)
				.append(groupRecord.scenarioName).append(tabulationSeparator)
				.append(groupRecord.groupName).append(tabulationSeparator)
				.append(groupRecord.userId.toString).append(tabulationSeparator)
				.append(groupRecord.event.name).append(tabulationSeparator)
				.append(groupRecord.executionDate.toString).append(eol)
			appendable
		}

		def append(requestRecord: RequestRecord): T = {
			appendable.append(ActionRecordType.name).append(tabulationSeparator)
				.append(requestRecord.scenarioName).append(tabulationSeparator)
				.append(requestRecord.userId.toString).append(tabulationSeparator)
				.append(requestRecord.requestName).append(tabulationSeparator)
				.append(requestRecord.executionStartDate.toString).append(tabulationSeparator)
				.append(requestRecord.requestSendingEndDate.toString).append(tabulationSeparator)
				.append(requestRecord.responseReceivingStartDate.toString).append(tabulationSeparator)
				.append(requestRecord.executionEndDate.toString).append(tabulationSeparator)
				.append(requestRecord.requestStatus.toString).append(tabulationSeparator)
				.append(requestRecord.requestMessage.getOrElse(emptyField))

			requestRecord.extraInfo.foreach(info => appendable.append(tabulationSeparator).append(sanitize(info.toString)))

			appendable.append(eol)

			appendable
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
	private var osw: OutputStreamWriter = _

	override def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {
		val simulationLog = simulationLogDirectory(runRecord.runId) / "simulation.log"
		osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(simulationLog.toString)), configuration.simulation.encoding)
		osw.append(RunRecordType.name).append(tabulationSeparator)
			.append(runRecord.timestamp).append(tabulationSeparator)
			.append(runRecord.simulationId).append(tabulationSeparator)
			// hack for being able to deserialize in FileDataReader
			.append(if (runRecord.runDescription.isEmpty) FileDataWriter.emptyField else runRecord.runDescription)
			.append(eol)
	}

	override def onScenarioRecord(scenarioRecord: ScenarioRecord) {
		osw.append(scenarioRecord)
	}

	override def onGroupRecord(groupRecord: GroupRecord) {
		osw.append(groupRecord)
	}

	override def onRequestRecord(requestRecord: RequestRecord) {
		osw.append(requestRecord)
	}

	override def onFlushDataWriter {

		logger.info("Received flush order")

		withCloseable(osw) { _.flush }
	}
}
