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
package io.gatling.core.controller

import java.util.UUID.randomUUID

import scala.collection.mutable
import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.util.{ Failure => SFailure, Success => SSuccess }

import org.joda.time.DateTime.now

import akka.actor.ActorRef
import akka.actor.ActorDSL.actor
import io.gatling.core.akka.{ AkkaDefaults, BaseActor }
import io.gatling.core.controller.throttle.{ Throttler, ThrottlingProtocol }
import io.gatling.core.result.message.{ End, Start }
import io.gatling.core.result.writer.{ DataWriter, RunMessage, UserMessage }
import io.gatling.core.util.TimeHelper.{ nanoTimeReference, nowMillis }

case class Timings(maxDuration: Option[FiniteDuration], globalThrottling: Option[ThrottlingProtocol], perScenarioThrottlings: Map[String, ThrottlingProtocol])

object Controller extends AkkaDefaults {

	val controller = actor(new Controller)
}

class Controller extends BaseActor {

	val activeUsers = mutable.Map.empty[String, UserMessage]
	var totalNumberOfUsers = 0
	var finishedUsers = 0

	var launcher: ActorRef = _
	var pendingDataWritersDone = 0
	var runId: String = _

	var timings: Timings = _

	var secondStartMillis = 0L

	var throttler: Throttler = _

	val uninitialized: Receive = {

		case Run(simulation, simulationId, description, timings) =>
			// important, initialize time reference
			val timeRef = nanoTimeReference
			launcher = sender
			this.timings = timings
			val scenarios = simulation.scenarios

			if (scenarios.isEmpty)
				launcher ! SFailure(new IllegalArgumentException(s"Simulation ${simulation.getClass} doesn't have any configured scenario"))

			else if (scenarios.map(_.name).toSet.size != scenarios.size)
				launcher ! SFailure(new IllegalArgumentException(s"Scenario names must be unique but found a duplicate"))

			else {
				totalNumberOfUsers = scenarios.map(_.injectionProfile.users).sum
				logger.info(s"Total number of users : $totalNumberOfUsers")

				val runMessage = RunMessage(simulation.getClass.getName, simulationId, now, description)
				runId = runMessage.runId
				DataWriter.askInit(runMessage, scenarios).onComplete {
					case f @ SFailure(_) => launcher ! f

					case SSuccess(dataWritersNumber) =>
						pendingDataWritersDone = dataWritersNumber
						val runUUID = math.abs(randomUUID.getMostSignificantBits)

						logger.debug("Launching All Scenarios")
						scenarios.foldLeft(0) { (i, scenario) =>
							scenario.run(runUUID + "-", i)
							i + scenario.injectionProfile.users
						}
						logger.debug("Finished Launching scenarios executions")

						val newState = if (timings.globalThrottling.isDefined || !timings.perScenarioThrottlings.isEmpty) {
							throttler = new Throttler(timings.globalThrottling, timings.perScenarioThrottlings)
							scheduler.schedule(0 seconds, 1 seconds, self, OneSecondTick)
							throttling.orElse(initialized)
						} else
							initialized

						context.become(newState)

						timings.maxDuration.foreach {
							scheduler.scheduleOnce(_) {
								self ! ForceTermination
							}
						}
				}
			}
	}

	val throttling: Receive = {

		case OneSecondTick => throttler.flushBuffer

		case ThrottledRequest(scenarioName, request) => throttler.send(scenarioName, request)
	}

	val initialized: Receive = {

		def terminate {
			launcher ! SSuccess(runId)
		}

		def dispatchUserStartToDataWriters(userMessage: UserMessage) {
			logger.info(s"Start user #${userMessage.userId}")
			DataWriter.tell(userMessage)
		}

		def dispatchUserEndToDataWriters(userMessage: UserMessage) {
			logger.info(s"End user #${userMessage.userId}")
			DataWriter.tell(userMessage)
		}

		{
			case userMessage @ UserMessage(_, userId, event, _, _) =>
				if (event == Start) {
					activeUsers += userId -> userMessage
					dispatchUserStartToDataWriters(userMessage)
				} else {
					finishedUsers += 1
					activeUsers -= userId
					dispatchUserEndToDataWriters(userMessage)
					if (finishedUsers == totalNumberOfUsers)
						DataWriter.askTerminate.onComplete(_ => terminate)
				}

			case DataWriterDone =>
				pendingDataWritersDone -= 1
				if (pendingDataWritersDone == 0) {
					scheduler.scheduleOnce(1 second) {
						terminate
					}
				}

			case ForceTermination =>
				// flush all active users
				val now = nowMillis
				for (activeUser <- activeUsers.values) {
					dispatchUserEndToDataWriters(activeUser.copy(event = End, endDate = now))
				}
				DataWriter.askTerminate.onComplete(_ => terminate)
		}
	}

	def receive = uninitialized
}
