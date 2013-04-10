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
package io.gatling.core.result.writer

import scala.concurrent.Future

import akka.actor.{ Actor, ActorRef, Props }
import io.gatling.core.action.{ AkkaDefaults, BaseActor, system }
import io.gatling.core.action.system.dispatcher
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.result.message.{ Flush, GroupMessage, Init, RequestMessage, RunMessage, ScenarioMessage, ShortScenarioDescription }
import io.gatling.core.result.terminator.Terminator
import io.gatling.core.scenario.Scenario

object DataWriter extends AkkaDefaults {

	private val dataWriters: Seq[ActorRef] = configuration.data.dataWriterClasses.map { className =>
		val clazz = Class.forName(className).asInstanceOf[Class[Actor]]
		system.actorOf(Props(clazz))
	}

	def tell(message: Any) {
		dataWriters.foreach(_ ! message)
	}

	def askInit(runMessage: RunMessage, scenarios: Seq[Scenario]) = {
		val shortScenarioDescriptions = scenarios.map(scenario => ShortScenarioDescription(scenario.name, scenario.configuration.users))

		val responses = dataWriters.map(_ ? Init(runMessage, shortScenarioDescriptions))

		Future.sequence(responses)
	}
}

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
trait DataWriter extends BaseActor {

	def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription])

	def onScenarioMessage(scenario: ScenarioMessage)

	def onGroupMessage(group: GroupMessage)

	def onRequestMessage(request: RequestMessage)

	def onFlushDataWriter

	def uninitialized: Receive = {
		case Init(runMessage, scenarios) =>

			logger.info("Initializing")

			val originalSender = sender

			Terminator.askDataWriterRegistration(self).onSuccess {
				case _ =>
					logger.info("Going on with initialization after Terminator registration")
					onInitializeDataWriter(runMessage, scenarios)
					context.become(initialized)
					originalSender ! true
					logger.info("Initialized")
			}
	}

	def initialized: Receive = {
		case scenarioMessage: ScenarioMessage => onScenarioMessage(scenarioMessage)

		case groupMessage: GroupMessage => onGroupMessage(groupMessage)

		case requestMessage: RequestMessage => onRequestMessage(requestMessage)

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