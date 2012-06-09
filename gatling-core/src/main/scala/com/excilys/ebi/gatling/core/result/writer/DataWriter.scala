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

import java.util.concurrent.CountDownLatch
import java.lang.System.currentTimeMillis

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.RequestStatus.OK
import com.excilys.ebi.gatling.core.result.message.{ RunRecord, RequestStatus, RequestRecord, InitializeDataWriter, FlushDataWriter }

import akka.actor.{ Props, ActorRef, Actor }

object DataWriter {

	private lazy val dataWriter: ActorRef = system.actorOf(Props(configuration.dataWriterClass))
	private val console: ActorRef = system.actorOf(Props(classOf[ConsoleDataWriter]))

	private def dispatch(message: Any) {
		dataWriter ! message
		console ! message
	}

	def init(runRecord: RunRecord, totalUsersCount: Int, latch: CountDownLatch, encoding: String) = dispatch(InitializeDataWriter(runRecord, totalUsersCount, latch, encoding))

	def startUser(scenarioName: String, userId: Int) = {
		val time = currentTimeMillis
		dispatch(RequestRecord(scenarioName, userId, START_OF_SCENARIO, time, time, time, time, OK, START_OF_SCENARIO))
	}

	def endUser(scenarioName: String, userId: Int) = {
		val time = currentTimeMillis
		dispatch(RequestRecord(scenarioName, userId, END_OF_SCENARIO, time, time, time, time, OK, END_OF_SCENARIO))
	}

	def askFlush = dispatch(FlushDataWriter)

  def logRequest(scenarioName: String, userId: Int, requestName: String,
                 executionStartDate: Long, executionEndDate: Long, requestSendingEndDate: Long, responseReceivingStartDate: Long,
                 requestResult: RequestStatus.RequestStatus, requestMessage: String, extraInfo: List[String] = List()) = {

    dispatch(RequestRecord(scenarioName, userId, requestName,
      executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate,
      requestResult, requestMessage, extraInfo))
  }
}

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
abstract class DataWriter extends Actor