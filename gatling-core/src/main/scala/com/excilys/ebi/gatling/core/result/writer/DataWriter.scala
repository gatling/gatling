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
import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.init.Initializable
import com.excilys.ebi.gatling.core.result.message.RequestStatus.OK
import com.excilys.ebi.gatling.core.result.message.{ RunRecord, RequestStatus, RequestRecord, InitializeDataWriter }
import akka.actor.{ PoisonPill, ActorRef, Actor }
import com.excilys.ebi.gatling.core.result.message.RequestRecord
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter
import com.excilys.ebi.gatling.core.action._
import com.excilys.ebi.gatling.core.result.message.RequestRecord
import akka.actor.Props
import com.excilys.ebi.gatling.core.result.message.InitializeDataWriter

object DataWriter {

	private lazy val instance: ActorRef = init

	private def init = system.actorOf(Props(configuration.dataWriterClass))

	def init(runRecord: RunRecord, totalUsersCount: Long, latch: CountDownLatch, encoding: String) = instance ! InitializeDataWriter(runRecord, totalUsersCount, latch, encoding)

	def startUser(scenarioName: String, userId: Int, time: Long) = DataWriter.instance ! RequestRecord(scenarioName, userId, START_OF_SCENARIO, time, time, time, time, OK, START_OF_SCENARIO)

	def endUser(scenarioName: String, userId: Int, time: Long) = DataWriter.instance ! RequestRecord(scenarioName, userId, END_OF_SCENARIO, time, time, time, time, OK, END_OF_SCENARIO)

	def askShutDown = instance ! PoisonPill

	def logRequest(scenarioName: String, userId: Int, requestName: String, executionStartDate: Long, executionEndDate: Long, requestSendingEndDate: Long, responseReceivingStartDate: Long, requestResult: RequestStatus.RequestStatus, requestMessage: String) =
		instance ! RequestRecord(scenarioName, userId, requestName, executionStartDate, executionEndDate, requestSendingEndDate, responseReceivingStartDate, requestResult, requestMessage)
}

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
abstract class DataWriter extends Actor with Initializable