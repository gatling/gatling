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
import com.excilys.ebi.gatling.core.util.StringHelper.END_OF_LINE

import akka.actor.scala2ActorRef

/**
 * File implementation of the DataWriter
 *
 * It writes the data of the simulation if a tabulation separated values file
 */
class FileDataWriter extends DataWriter {

	/**
	 * The OutputStreamWriter used to write to files
	 */
	var osw: OutputStreamWriter = _
	/**
	 * The countdown latch that will be decreased when all messaged are written and all scenarios ended
	 */
	var latch: CountDownLatch = _
	/**
	 * The date on which the simulation started
	 */
	var runOn: String = _

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
		case InitializeDataWriter(runOn, latch) => {

			if (initialized.compareAndSet(false, true)) {
				this.runOn = printFileNameDate(runOn)
				// Initialize files and folders that will be used to write the logs
				Directory(resultFolder(this.runOn)).createDirectory()

				osw = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(File(simulationLogFile(this.runOn)).jfile, true)))

				ResultLine.Headers.print(osw).append(END_OF_LINE)

				this.latch = latch

				// the latch is set to totalUsersCount + 1 so main thread awaits until this FileDataWriter is closed
				totalUsersCount.set(latch.getCount - 1)

			} else {
				logger.error("FileDataWriter has already been initialized!")
			}
		}

		// If the message comes from an action
		case ActionInfo(scenarioName, userId, action, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) => {
			if (initialized.get == true) {
				// Write the line in the file
				new ResultLine(runOn, scenarioName, userId, action, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage).print(osw).append(END_OF_LINE)

				action match {
					case START_OF_SCENARIO => activeUsersCount.incrementAndGet
					case END_OF_SCENARIO => activeUsersCount.decrementAndGet
					case _ => resultStatus match {
						case OK => successfulRequestsCount.incrementAndGet
						case KO => failedRequestsCount.incrementAndGet
					}
				}

				// print sample stats
				// not thread safe but not critical either
				val now = currentTimeMillis
				if (now - lastDisplayTime > displayPeriod) {
					lastDisplayTime = now
					val timeSinceStartUpInSec = (now - startUpTime) / 1000
					println(new StringBuilder("Running for ").append(timeSinceStartUpInSec).append(" sec | Users: active=").append(activeUsersCount.get).append("/").append(totalUsersCount.get).append(" | Requests: OK=").append(successfulRequestsCount.get).append(" KO=").append(failedRequestsCount.get))
				}

				if (latch.getCount == 1 && self.dispatcher.mailboxSize(self) == 0) {
					try {
						// Closes the OutputStreamWriter
						osw.flush
					} finally {
						// Decrease the latch (should be at 0 here)
						latch.countDown
						osw.close
					}
				}
			} else {
				logger.error("FileDataWriter hasn't been initialized!")
			}
		}

		case unknown =>
			logger.error("Unknow message type " + unknown.getClass)
	}
}
