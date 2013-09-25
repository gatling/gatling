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
package io.gatling.core.result.writer

import scala.concurrent.Future

import akka.actor.{ Actor, ActorRef, Props }
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.controller.{ Controller, DataWriterDone }
import io.gatling.core.result.message.End
import io.gatling.core.scenario.Scenario

case class InitDataWriter(totalNumberOfUsers: Int)

object DataWriter extends AkkaDefaults {

	private val dataWriters: Seq[ActorRef] = configuration.data.dataWriterClasses.map { className =>
		val clazz = Class.forName(className).asInstanceOf[Class[Actor]]
		system.actorOf(Props(clazz))
	}

	def tell(message: Any) {
		dataWriters.foreach(_ ! message)
	}

	def askInit(runMessage: RunMessage, totalNumberOfUsers: Int, scenarios: Seq[Scenario]): Future[Int] = {
		val shortScenarioDescriptions = scenarios.map(scenario => ShortScenarioDescription(scenario.name, scenario.injectionProfile.users))
		val responses = dataWriters.map(_ ? Init(runMessage, totalNumberOfUsers, shortScenarioDescriptions))
		Future.sequence(responses).map(_.size)
	}

	def askTerminate(): Future[Int] = {
		val responses = dataWriters.map(_ ? Terminate)
		Future.sequence(responses).map(_.size)
	}
}

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
abstract class DataWriter extends BaseActor {

	var pendingEndUserMessages: Int = 0

	def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription])

	def onScenarioMessage(scenario: ScenarioMessage)

	def onGroupMessage(group: GroupMessage)

	def onRequestMessage(request: RequestMessage)

	def onTerminateDataWriter

	private def doTerminate() {
		try {
			onTerminateDataWriter
		} finally {
			context.become(flushed)
			sender ! true
		}
	}

	def uninitialized: Receive = {
		case Init(runMessage, totalNumberOfUsers, scenarios) =>
			logger.info("Initializing")
			pendingEndUserMessages = totalNumberOfUsers
			onInitializeDataWriter(runMessage, scenarios)
			logger.info("Initialized")
			context.become(initialized)
			sender ! true

		case m: DataWriterMessage => logger.error(s"Can't handle $m when in uninitialized state, discarding")
	}

	def initialized: Receive = {
		case scenarioMessage: ScenarioMessage =>
			onScenarioMessage(scenarioMessage)
			if (scenarioMessage.event == End) {
				pendingEndUserMessages -= 1
				if (pendingEndUserMessages == 0)
					try {
						onTerminateDataWriter
					} finally {
						context.become(flushed)
						logger.info("Terminated")
						Controller.controller ! DataWriterDone
					}
			}

		case groupMessage: GroupMessage => onGroupMessage(groupMessage)

		case requestMessage: RequestMessage => onRequestMessage(requestMessage)

		case Terminate => doTerminate
	}

	def flushed: Receive = {
		case m => logger.info(s"Can't handle $m after being flush")
	}

	def receive = uninitialized
}
