/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
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
import scala.concurrent.duration.DurationInt

import akka.actor.{ Actor, ActorRef, Props }
import akka.util.Timeout
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.controller.{ DataWritersInitialized, DataWritersTerminated }
import io.gatling.core.scenario.Scenario

case class InitDataWriter(totalNumberOfUsers: Int)

object DataWriter extends AkkaDefaults {

	implicit val dataWriterTimeOut = Timeout(5 seconds)

	private var _instances: Option[Seq[ActorRef]] = None

	def instances() = _instances match {
		case Some(dw) => dw
		case _ => throw new UnsupportedOperationException("DataWriters haven't been initialized")
	}

	def tell(message: Any) {
		instances.foreach(_ ! message)
	}

	def init(runMessage: RunMessage, scenarios: Seq[Scenario], replyTo: ActorRef) {

		_instances = {
			val dw = configuration.data.dataWriterClasses.map { className =>
				val clazz = Class.forName(className).asInstanceOf[Class[Actor]]
				system.actorOf(Props(clazz))
			}

			system.registerOnTermination(_instances = None)

			Some(dw)
		}

		val shortScenarioDescriptions = scenarios.map(scenario => ShortScenarioDescription(scenario.name, scenario.injectionProfile.users))
		val responses = instances.map(_ ? Init(runMessage, shortScenarioDescriptions))
		Future.sequence(responses).map(_ => {}).onComplete(replyTo ! DataWritersInitialized(_))
	}

	def terminate(replyTo: ActorRef) {
		val responses = instances.map(_ ? Terminate)
		Future.sequence(responses).map(_ => {}).onComplete(replyTo ! DataWritersTerminated(_))
	}
}

/**
 * Abstract class for all DataWriters
 *
 * These writers are responsible for writing the logs that will be read to
 * generate the statistics
 */
abstract class DataWriter extends BaseActor {

	def onInitializeDataWriter(run: RunMessage, scenarios: Seq[ShortScenarioDescription])

	def onUserMessage(userMessage: UserMessage)

	def onGroupMessage(group: GroupMessage)

	def onRequestMessage(request: RequestMessage)

	def onTerminateDataWriter

	def uninitialized: Receive = {
		case Init(runMessage, scenarios) =>
			logger.info("Initializing")
			onInitializeDataWriter(runMessage, scenarios)
			logger.info("Initialized")
			context.become(initialized)
			sender ! true

		case m: DataWriterMessage => logger.error(s"Can't handle $m when in uninitialized state, discarding")
	}

	def initialized: Receive = {
		case userMessage: UserMessage => onUserMessage(userMessage)

		case groupMessage: GroupMessage => onGroupMessage(groupMessage)

		case requestMessage: RequestMessage => onRequestMessage(requestMessage)

		case Terminate => try {
			onTerminateDataWriter
		} finally {
			context.become(flushed)
			sender ! true
		}
	}

	def flushed: Receive = {
		case m => logger.info(s"Can't handle $m after being flush")
	}

	def receive = uninitialized
}
