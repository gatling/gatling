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
import java.lang.System.currentTimeMillis
import java.util.concurrent.CountDownLatch

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingFiles.simulationLogFile
import com.excilys.ebi.gatling.core.result.message.RecordType.{ RUN, ACTION }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
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

	private val startUpTime = currentTimeMillis

	private var activeUsersCount = 0

	private var totalUsersCount = 0L

	private var successfulRequestsCount = 0

	private var failedRequestsCount = 0

	private var lastDisplayTime = currentTimeMillis

	private val displayPeriod = 5 * 1000

	/**
	 * Method called when this actor receives a message
	 */
	def receive = {
		// If the message is sent to initialize the writer
		case InitializeDataWriter(runRecord, totalUsersCount, latch, encoding) => {

			def initStreamWriter {
				osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(simulationLogFile(runRecord.runUuid).toString)), encoding)
			}

			def printRunRecord {
				osw.append(RUN).append(TABULATION_SEPARATOR)
					.append(toTimestamp(runRecord.runDate)).append(TABULATION_SEPARATOR)
					.append(runRecord.runId).append(TABULATION_SEPARATOR)
					// hack for being able to deserialize in FileDataReader
					.append(if (runRecord.runDescription.isEmpty) " " else runRecord.runDescription)
					.append(END_OF_LINE)
			}

			if (initialized.compareAndSet(false, true)) {
				this.latch = latch
				this.totalUsersCount = totalUsersCount
				initStreamWriter
				printRunRecord

			} else {
				error("FileDataWriter has already been initialized!")
			}
		}

		// If the message comes from an action
		case RequestRecord(scenarioName, userId, actionName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) => {

			def printRequestRecord {
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
			}

			def handleCounters {
				actionName match {
					case START_OF_SCENARIO => activeUsersCount += 1
					case END_OF_SCENARIO => activeUsersCount -= 1
					case _ => resultStatus match {
						case OK => successfulRequestsCount += 1
						case KO => failedRequestsCount += 1
					}
				}
			}

			def displaySamplingInfo {
				// not thread safe but not critical either
				val now = currentTimeMillis
				if (now - lastDisplayTime > displayPeriod) {
					lastDisplayTime = now
					val timeSinceStartUpInSec = (now - startUpTime) / 1000
					println(new StringBuilder()
						.append(timeSinceStartUpInSec)
						.append(" sec | Users: active=")
						.append(activeUsersCount)
						.append("/")
						.append(totalUsersCount)
						.append(" | Requests: OK=")
						.append(successfulRequestsCount)
						.append(" KO=")
						.append(failedRequestsCount))
				}
			}

			if (initialized.get) {
				printRequestRecord
				handleCounters
				displaySamplingInfo

			} else {
				error("FileDataWriter hasn't been initialized!")
			}
		}

		case FlushDataWriter => {
			info("Received flush order")

			try {
				osw.flush
			} finally {
				// Decrease the latch (should be at 0 here)
				initialized.set(false)
				osw.close
				latch.countDown
			}
		}
		case unknown: AnyRef => error("Unknow message type " + unknown.getClass)
		case unknown: Any => error("Unknow message type " + unknown)
	}
}
