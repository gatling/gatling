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

import com.excilys.ebi.gatling.core.init.Initializable
import akka.actor.Actor
import java.lang.System.currentTimeMillis
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import grizzled.slf4j.Logging
import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.result.message.RequestStatus.{ OK, KO }
import com.excilys.ebi.gatling.core.result.message.FlushDataWriter

class ConsoleDataWriter extends DataWriter with Logging {

	private var startUpTime: Long = _
	private var lastDisplayTime: Long = _
	private var activeUsersCount: Int = _
	private var totalUsersCount: Int = _
	private var successfulRequestsCount: Int = _
	private var failedRequestsCount: Int = _

	private val displayPeriod = 5 * 1000

	/**
	 * Method called when this actor receives a message
	 */
	def receive = {
		// If the message is sent to initialize the writer
		case InitializeDataWriter(_, totalUsersCount, _, _) => {

			if (initialized.compareAndSet(false, true)) {
				startUpTime = currentTimeMillis
				activeUsersCount = 0
				successfulRequestsCount = 0
				failedRequestsCount = 0
				lastDisplayTime = currentTimeMillis
				this.totalUsersCount = totalUsersCount

			} else {
				error("FileDataWriter has already been initialized!")
			}
		}

		// If the message comes from an action
		case RequestRecord(scenarioName, userId, actionName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, resultStatus, resultMessage) => {

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
				handleCounters
				displaySamplingInfo

			} else {
				error("FileDataWriter hasn't been initialized!")
			}
		}

		case FlushDataWriter => initialized.set(false)
		case unknown: AnyRef => error("Unknow message type " + unknown.getClass)
		case unknown: Any => error("Unknow message type " + unknown)
	}
}