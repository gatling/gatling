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

import java.io.{ BufferedOutputStream, FileOutputStream, OutputStreamWriter }

import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogDirectory
import com.excilys.ebi.gatling.core.result.message.GroupRecord
import com.excilys.ebi.gatling.core.result.message.RecordType.{ ACTION, GROUP, RUN, SCENARIO }
import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.result.message.RunRecord
import com.excilys.ebi.gatling.core.result.message.ScenarioRecord
import com.excilys.ebi.gatling.core.result.message.ShortScenarioDescription
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR
import com.excilys.ebi.gatling.core.util.IOHelper.use
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

import grizzled.slf4j.Logging

object FileDataWriter {

	val emptyField = " "

	val sanitizerPattern = """[\n\r\t]""".r

	private[writer] def append(appendable: Appendable, requestRecord: RequestRecord) {

		appendable.append(ACTION).append(TABULATION_SEPARATOR)
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
			appendable.append(TABULATION_SEPARATOR).append(sanitize(info))
		})

		appendable.append(END_OF_LINE)
	}

	/**
	 * Converts whitespace characters that would break the simulation log format into spaces.
	 * @param input
	 * @return
	 */
	private[writer] def sanitize(input: String): String = Option(sanitizerPattern.replaceAllIn(input, " ")).getOrElse("")
}

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter with Logging {

	/**
	 * The OutputStreamWriter used to write to files
	 */
	private var osw: OutputStreamWriter = _

	override def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription]) {
		val simulationLog = simulationLogDirectory(runRecord.runId) / "simulation.log"
		osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(simulationLog.toString)), configuration.core.encoding)
		osw.append(RUN).append(TABULATION_SEPARATOR)
			.append(runRecord.timestamp).append(TABULATION_SEPARATOR)
			.append(runRecord.simulationId).append(TABULATION_SEPARATOR)
			// hack for being able to deserialize in FileDataReader
			.append(if (runRecord.runDescription.isEmpty) FileDataWriter.emptyField else runRecord.runDescription)
			.append(END_OF_LINE)
	}


	override def onScenarioRecord(scenarioRecord: ScenarioRecord) {
		osw.append(SCENARIO).append(TABULATION_SEPARATOR)
			.append(scenarioRecord.scenarioName).append(TABULATION_SEPARATOR)
			.append(scenarioRecord.userId.toString).append(TABULATION_SEPARATOR)
			.append(scenarioRecord.event).append(TABULATION_SEPARATOR)
			.append(scenarioRecord.executionDate.toString).append(END_OF_LINE)
	}

	override def onGroupRecord(groupRecord: GroupRecord) {
		osw.append(GROUP).append(TABULATION_SEPARATOR)
			.append(groupRecord.scenarioName).append(TABULATION_SEPARATOR)
			.append(groupRecord.groupName).append(TABULATION_SEPARATOR)
			.append(groupRecord.userId.toString).append(TABULATION_SEPARATOR)
			.append(groupRecord.event).append(TABULATION_SEPARATOR)
			.append(groupRecord.executionDate.toString).append(END_OF_LINE)
	}

	override def onRequestRecord(requestRecord: RequestRecord) {
		FileDataWriter.append(osw, requestRecord)
	}

	override def onFlushDataWriter {

		info("Received flush order")

		use(osw) { _.flush }
	}
}
