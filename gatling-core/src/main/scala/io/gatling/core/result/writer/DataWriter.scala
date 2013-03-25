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
package com.excilys.ebi.gatling.core.result.writer

import scala.concurrent.Future

import com.excilys.ebi.gatling.core.action.{ AkkaDefaults, BaseActor, system }
import com.excilys.ebi.gatling.core.action.system.dispatcher
import com.excilys.ebi.gatling.core.config.GatlingConfiguration.configuration
import com.excilys.ebi.gatling.core.result.message.{ Flush, GroupRecord, Init, RecordEvent, RequestRecord, RequestStatus, RunRecord, ScenarioRecord, ShortScenarioDescription }
import com.excilys.ebi.gatling.core.result.terminator.Terminator
import com.excilys.ebi.gatling.core.scenario.Scenario
import com.excilys.ebi.gatling.core.util.TimeHelper.nowMillis

import akka.actor.{ Actor, ActorRef, Props }
import grizzled.slf4j.Logging

object DataWriter extends AkkaDefaults with Logging {

	private val dataWriters: Seq[ActorRef] = configuration.data.dataWriterClasses.map { className =>
		val clazz = Class.forName(className).asInstanceOf[Class[Actor]]
		system.actorOf(Props(clazz))
	}

	private def tellAll(message: Any) {
		dataWriters.foreach(_ ! message)
	}

	def askInit(runRecord: RunRecord, scenarios: Seq[Scenario]) = {
		val shortScenarioDescriptions = scenarios.map(scenario => ShortScenarioDescription(scenario.name, scenario.configuration.users))

		val responses = dataWriters.map(_ ? Init(runRecord, shortScenarioDescriptions))

		Future.sequence(responses)
	}

	def user(scenarioName: String, userId: Int, event: RecordEvent) {
		tellAll(ScenarioRecord(scenarioName, userId, event, nowMillis))
	}

	def group(scenarioName: String, groupName: String, userId: Int, event: RecordEvent) {
		tellAll(GroupRecord(scenarioName, groupName, userId, event, nowMillis))
	}

	def logRequest(
		scenarioName: String,
		userId: Int,
		requestName: String,
		executionStartDate: Long,
		requestSendingEndDate: Long,
		responseReceivingStartDate: Long,
		executionEndDate: Long,
		requestResult: RequestStatus,
		requestMessage: Option[String] = None,
		extraInfo: List[Any] = Nil) {

		tellAll(RequestRecord(
			scenarioName,
			userId,
			requestName,
			executionStartDate,
			requestSendingEndDate,
			responseReceivingStartDate,
			executionEndDate,
			requestResult,
			requestMessage,
			extraInfo))
	}
}

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
trait DataWriter extends BaseActor {

	def onInitializeDataWriter(runRecord: RunRecord, scenarios: Seq[ShortScenarioDescription])

	def onScenarioRecord(scenarioRecord: ScenarioRecord)

	def onGroupRecord(groupRecord: GroupRecord)

	def onRequestRecord(requestRecord: RequestRecord)

	def onFlushDataWriter

	def uninitialized: Receive = {
		case Init(runRecord, scenarios) =>

			info("Initializing")

			val originalSender = sender

			Terminator.askDataWriterRegistration(self).onSuccess {
				case _ =>
					info("Going on with initialization after Terminator registration")
					onInitializeDataWriter(runRecord, scenarios)
					context.become(initialized)
					originalSender ! true
					info("Initialized")
			}
	}

	def initialized: Receive = {
		case scenarioRecord: ScenarioRecord => onScenarioRecord(scenarioRecord)

		case groupRecord: GroupRecord => onGroupRecord(groupRecord)

		case requestRecord: RequestRecord => onRequestRecord(requestRecord)

		case Flush =>
			try {
				onFlushDataWriter
			} finally {
				context.unbecome
				sender ! true
			}
	}

	def receive = uninitialized
}