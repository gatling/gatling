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
package io.gatling.core.runner

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.{ Failure => SFailure, Success => SSuccess }

import com.typesafe.scalalogging.slf4j.StrictLogging

import akka.util.Timeout
import io.gatling.core.akka.{ AkkaDefaults, GatlingActorSystem }
import io.gatling.core.config.GatlingConfiguration.configuration
import io.gatling.core.controller.{ Controller, Run }
import io.gatling.core.scenario.Simulation

class Runner(selection: Selection) extends AkkaDefaults with StrictLogging {

	def run: (String, Simulation) = {

		try {
			val simulationClass = selection.simulationClass
			println(s"Simulation ${simulationClass.getName} started...")

			val simulation = simulationClass.newInstance
			GatlingActorSystem.start
			Controller.start

			simulation._beforeSteps.foreach(_.apply)

			//override defaultTimeOut
			implicit val defaultTimeOut = Timeout(configuration.core.timeOut.simulation seconds)
			val runResult = Controller ? Run(simulation, selection.simulationId, selection.description, simulation.timings)

			Await.result(runResult, defaultTimeOut.duration) match {
				case SSuccess(runId: String) =>
					println("Simulation finished")
					simulation._afterSteps.foreach(_.apply)
					(runId, simulation)

				case SFailure(t) => throw t
				case unexpected => throw new UnsupportedOperationException(s"Controller replied an unexpected message $unexpected")
			}

		} finally {
			GatlingActorSystem.shutdown
		}
	}
}
