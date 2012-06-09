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
package com.excilys.ebi.gatling.core.result.writer

import java.io.{ OutputStreamWriter, FileOutputStream, BufferedOutputStream }
import java.util.concurrent.CountDownLatch

import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogDirectory
import com.excilys.ebi.gatling.core.result.message.RecordType.{ RUN, ACTION }
import com.excilys.ebi.gatling.core.result.message.{ RequestRecord, InitializeDataWriter, FlushDataWriter }
import com.excilys.ebi.gatling.core.util.DateHelper.toTimestamp
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

import grizzled.slf4j.Logging

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

	/**
	 * The countdown latch that will be decreased when all message are written and all scenarios ended
	 */
	private var latch: CountDownLatch = _

	def uninitialized: Receive = {
		case InitializeDataWriter(runRecord, totalUsersCount, latch, encoding) =>
			this.latch = latch
			val simulationLog = simulationLogDirectory(runRecord.runUuid) / "simulation.log"
			osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(simulationLog.toString)), encoding)
			osw.append(RUN).append(TABULATION_SEPARATOR)
				.append(toTimestamp(runRecord.runDate)).append(TABULATION_SEPARATOR)
				.append(runRecord.runId).append(TABULATION_SEPARATOR)
				// hack for being able to deserialize in FileDataReader
				.append(if (runRecord.runDescription.isEmpty) " " else runRecord.runDescription)
				.append(END_OF_LINE)
			context.become(initialized)

		case unknown: AnyRef => error("Unsupported message type in uninilialized state" + unknown.getClass)
		case unknown: Any => error("Unsupported message type in uninilialized state " + unknown)
	}

	def initialized: Receive = {
		case RequestRecord(scenarioName, userId, actionName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage, extraRequestInfo) =>
			osw.append(ACTION).append(TABULATION_SEPARATOR)
				.append(scenarioName).append(TABULATION_SEPARATOR)
				.append(userId.toString).append(TABULATION_SEPARATOR)
				.append(actionName).append(TABULATION_SEPARATOR)
				.append(executionStartDate.toString).append(TABULATION_SEPARATOR)
				.append(executionEndDate.toString).append(TABULATION_SEPARATOR)
				.append(requestSendingEndDate.toString).append(TABULATION_SEPARATOR)
				.append(responseReceivingStartDate.toString).append(TABULATION_SEPARATOR)
				.append(resultStatus.toString).append(TABULATION_SEPARATOR)
				.append(resultMessage)
				.append(END_OF_LINE)

		case FlushDataWriter =>
			info("Received flush order")

			try {
				osw.flush
			} finally {
				context.unbecome() // return to uninitialized state
				// Decrease the latch (should be at 0 here)
				osw.close
				latch.countDown
			}

		case unknown: AnyRef => error("Unsupported message type in inilialized state " + unknown.getClass)
		case unknown: Any => error("Unsupported message type in inilialized state " + unknown)
	}

	def receive = uninitialized
}
