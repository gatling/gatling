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

import java.util.UUID

import scala.concurrent.duration.{ DurationInt, FiniteDuration }
import scala.util.{ Failure => SFailure, Success => SSuccess }

import org.joda.time.DateTime.now

import akka.actor.ActorRef
import akka.actor.ActorDSL.actor
import io.gatling.core.action.{ AkkaDefaults, BaseActor }
import io.gatling.core.result.writer.{ DataWriter, RunMessage }
import io.gatling.core.util.TimeHelper

case class Timings(maxDuration: Option[FiniteDuration])

object Controller extends AkkaDefaults {

	val controller = actor(new Controller)
}

class Controller extends BaseActor {

	import context._

	var caller: ActorRef = _
	var pendingDataWritersDone = 0
	var runId: String = _

	val uninitialized: Receive = {

		case Run(simulation, simulationId, description, timings) =>
			// important, initialize time reference
			val timeRef = TimeHelper.nanoTimeReference
			caller = sender
			val scenarios = simulation.scenarios

			if (scenarios.isEmpty)
				caller ! SFailure(new IllegalArgumentException(s"Simulation ${simulation.getClass} doesn't have any configured scenario"))

			else if (scenarios.map(_.name).toSet.size != scenarios.size)
				caller ! SFailure(new IllegalArgumentException(s"Scenario names must be unique but found a duplicate"))

			else {
				val totalNumberOfUsers = scenarios.map(_.injectionProfile.users).sum
				logger.info(s"Total number of users : $totalNumberOfUsers")

				val runMessage = RunMessage(now, simulationId, description)
				runId = runMessage.runId
				DataWriter.askInit(runMessage, totalNumberOfUsers, scenarios).onComplete {
					case f @ SFailure(_) => caller ! f

					case SSuccess(dataWritersNumber) =>
						pendingDataWritersDone = dataWritersNumber
						val runUUID = UUID.randomUUID.getMostSignificantBits

						logger.debug("Launching All Scenarios")
						scenarios.foldLeft(0) { (i, scenario) =>
							scenario.run(runUUID + "-", i)
							i + scenario.injectionProfile.users
						}

						timings.maxDuration.foreach {
							system.scheduler.scheduleOnce(_) {
								self ! ForceTermination
							}
						}

						logger.debug("Finished Launching scenarios executions")
						context.become(initialized)
				}
			}
	}

	val initialized: Receive = {

		def terminate {
			caller ! SSuccess(runId)
		}

		{
			case DataWriterDone =>
				pendingDataWritersDone -= 1
				if (pendingDataWritersDone == 0) {
					logger.info("Terminating")
					system.scheduler.scheduleOnce(1 second) {
						terminate
					}
				}

			case ForceTermination =>
				DataWriter.askTerminate.onComplete(_ => terminate)
		}
	}

	def receive = uninitialized
}