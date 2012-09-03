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

import com.excilys.ebi.gatling.core.action.EndAction.END_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.LoggingActor
import com.excilys.ebi.gatling.core.action.StartAction.START_OF_SCENARIO
import com.excilys.ebi.gatling.core.action.system
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.{ FlushDataWriter, InitializeDataWriter, RequestRecord, RequestStatus }
import com.excilys.ebi.gatling.core.result.message.{ RunRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.message.RequestStatus.OK
import com.excilys.ebi.gatling.core.result.terminator.Terminator
import com.excilys.ebi.gatling.core.scenario.Scenario
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis

import akka.actor.{ Actor, ActorRef, Props }
import akka.routing.BroadcastRouter

object DataWriter {

	private val dataWriters: Seq[ActorRef] = configuration.data.dataWriterClasses.map { className =>
		val clazz = Class.forName(className).asInstanceOf[Class[Actor]]
		system.actorOf(Props(clazz))
	}

	private val router = system.actorOf(Props[Actor].withRouter(BroadcastRouter(routees = dataWriters)))

	def init(runRecord: RunRecord, scenarios: Seq[Scenario]) = {
		val shortScenarioDescriptions = scenarios.map(scenario => ShortScenarioDescription(scenario.name, scenario.configuration.users))
		router ! InitializeDataWriter(runRecord, shortScenarioDescriptions)
	}

	def startUser(scenarioName: String, userId: Int) = {
		val time = nowMillis
		router ! RequestRecord(scenarioName, userId, START_OF_SCENARIO, time, time, time, time, OK)
	}

	def endUser(scenarioName: String, userId: Int) = {
		val time = nowMillis
		router ! RequestRecord(scenarioName, userId, END_OF_SCENARIO, time, time, time, time, OK)
	}

	def logRequest(
		scenarioName: String,
		userId: Int,
		requestName: String,
		executionStartDate: Long,
		executionEndDate: Long,
		requestSendingEndDate: Long,
		responseReceivingStartDate: Long,
		requestResult: RequestStatus.RequestStatus,
		requestMessage: Option[String] = None,
		extraInfo: List[String] = Nil) = {

		router ! RequestRecord(
			scenarioName,
			userId,
			requestName,
			executionStartDate,
			executionEndDate,
			requestSendingEndDate,
			responseReceivingStartDate,
			requestResult,
			requestMessage,
			extraInfo)
	}
}

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
abstract class DataWriter extends LoggingActor {

	def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription])

	def onRequestRecord(requestRecord: RequestRecord)

	def onFlushDataWriter

	def uninitialized: Receive = {
		case InitializeDataWriter(runRecord, scenarios) =>

			Terminator.registerDataWriter(self)
			onInitializeDataWriter(runRecord, scenarios)
			context.become(initialized)
	}

	def initialized: Receive = {
		case requestRecord: RequestRecord => onRequestRecord(requestRecord)

		case FlushDataWriter =>
			try {
				onFlushDataWriter
			} finally {
				context.unbecome
				sender ! true
			}
	}

	def receive = uninitialized
}