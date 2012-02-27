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
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.CountDownLatch
import scala.tools.nsc.io.{ File, Directory }
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingFiles.{ simulationLogFile, resultFolder }
import com.excilys.ebi.gatling.core.result.message.ResultStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.message.{ InitializeDataWriter, ActionInfo }
import com.excilys.ebi.gatling.core.util.DateHelper.printFileNameDate
import com.excilys.ebi.gatling.core.util.StringHelper.{ END_OF_LINE, EMPTY }
import com.excilys.ebi.gatling.core.util.FileHelper.TABULATION_SEPARATOR
import akka.actor.scala2ActorRef
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
	var osw: OutputStreamWriter = _
	/**
	 * The countdown latch that will be decreased when all messaged are written and all scenarios ended
	 */
	var latch: CountDownLatch = _

	val startUpTime = currentTimeMillis

	val activeUsersCount = new AtomicLong(0)

	val totalUsersCount = new AtomicLong(0)

	val successfulRequestsCount = new AtomicLong(0)

	val failedRequestsCount = new AtomicLong(0)

	@volatile var lastDisplayTime = currentTimeMillis

	val displayPeriod = 5 * 1000

	/**
	 * Method called when this actor receives a message
	 */
	def receive = {

		// If the message is sent to initialize the writer
		case InitializeDataWriter(runInfo, latch) => {

			def initStreamWriter {
				osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(File(simulationLogFile(runInfo.runUuid)).jfile, true)))
			}

			def printRunInfo {
				osw.append(ResultLine.RunInfoHeaders.RUN_DATE).append(TABULATION_SEPARATOR).append(printFileNameDate(runInfo.runDate)).append(END_OF_LINE)
				osw.append(ResultLine.RunInfoHeaders.RUN_ID).append(TABULATION_SEPARATOR).append(runInfo.runId).append(END_OF_LINE)
				osw.append(ResultLine.RunInfoHeaders.RUN_NAME).append(TABULATION_SEPARATOR).append(runInfo.runName).append(END_OF_LINE)
			}

			def printDataHeaders {
				ResultLine.Headers.print(osw).append(END_OF_LINE)
			}

			def handleCounters {
				// the latch is set to totalUsersCount + 1 so main thread awaits until this FileDataWriter is closed
				totalUsersCount.set(latch.getCount - 1)
			}

			if (initialized.compareAndSet(false, true)) {
				this.latch = latch

				initStreamWriter
				printRunInfo
				printDataHeaders
				handleCounters

			} else {
				error("FileDataWriter has already been initialized!")
			}
		}

		// If the message comes from an action
		case ActionInfo(scenarioName, userId, action, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) => {

			def printResultLine {
				// Write the line in the file
				new ResultLine(scenarioName, userId, action, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage).print(osw).append(END_OF_LINE)
			}

			def handleCounters {
				action match {
					case START_OF_SCENARIO => activeUsersCount.incrementAndGet
					case END_OF_SCENARIO => activeUsersCount.decrementAndGet
					case _ => resultStatus match {
						case OK => successfulRequestsCount.incrementAndGet
						case KO => failedRequestsCount.incrementAndGet
					}
				}
			}

			def displaySamplingInfo {
				// not thread safe but not critical either
				val now = currentTimeMillis
				if (now - lastDisplayTime > displayPeriod) {
					lastDisplayTime = now
					val timeSinceStartUpInSec = (now - startUpTime) / 1000
					println(new StringBuilder().append(timeSinceStartUpInSec).append(" sec | Users: active=").append(activeUsersCount.get).append("/").append(totalUsersCount.get).append(" | Requests: OK=").append(successfulRequestsCount.get).append(" KO=").append(failedRequestsCount.get))
				}
			}

			def closeIfLastMessage {
				if (latch.getCount == 1 && self.dispatcher.mailboxIsEmpty(self)) {
					try {
						// Closes the OutputStreamWriter
						osw.flush
					} finally {
						// Decrease the latch (should be at 0 here)
						latch.countDown
						osw.close
					}
				}
			}

			if (initialized.get) {
				printResultLine
				handleCounters
				displaySamplingInfo
				closeIfLastMessage

			} else {
				error("FileDataWriter hasn't been initialized!")
			}
		}

		case unknown => error("Unknow message type " + unknown.getClass)
	}
}
