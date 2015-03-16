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

import io.gatling.core.action.UserEnd
import io.gatling.core.config.{ Protocols, GatlingConfiguration }
import io.gatling.core.result.writer.{ RunMessage, DataWriters }

import scala.concurrent.{ Await, TimeoutException }
import scala.concurrent.duration._
import scala.util.{ Try, Failure, Success }

import com.typesafe.scalalogging.StrictLogging

import akka.util.Timeout
import io.gatling.core.akka.{ AkkaDefaults, GatlingActorSystem }
import io.gatling.core.controller.{ Run, Controller }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.util.TimeHelper._

case class RunResult(runId: String, hasAssertions: Boolean)

class Runner(selection: Selection)(implicit configuration: GatlingConfiguration) extends AkkaDefaults with StrictLogging {

  def run: RunResult =
    try {
      val simulationClass = selection.simulationClass
      println(s"Simulation ${simulationClass.getName} started...")

      // important, initialize time reference
      val timeRef = NanoTimeReference

      // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
      GatlingActorSystem.start()
      val simulation = simulationClass.newInstance

      simulation._beforeSteps.foreach(_.apply())

      val runMessage = RunMessage(selection.simulationClass.getName, selection.simulationId, nowMillis, selection.description)

      val dataWritersInit = DataWriters(simulation._populationBuilders, simulation._assertions, selection, runMessage)
      val dataWriters = Await.result(dataWritersInit, 5 seconds).get

      val controller = Controller(selection, dataWriters)
      val userEnd = UserEnd(controller)

      val simulationDef = simulation.build(controller, dataWriters, userEnd)

      simulationDef.scenarios.foldLeft(Protocols()) { (protocols, scenario) =>
        protocols ++ scenario.ctx.protocols
      }.warmUp(dataWriters)

      Throttler.start(simulationDef)

      val simulationTimeout = configuration.core.timeout.simulation seconds
      implicit val timeout = Timeout(simulationTimeout)

      System.gc()
      System.gc()
      System.gc()

      val runResult = (controller ? Run(simulationDef)).mapTo[Try[String]]

      val res = try {
        Await.result(runResult, simulationTimeout)
      } catch {
        case t: TimeoutException => throw new TimeoutException(s"Reach simulation timeout of $timeout")
      }

      res match {
        case Success(_) =>
          println("Simulation finished")
          simulation._afterSteps.foreach(_.apply())
          RunResult(runMessage.runId, simulationDef.assertions.nonEmpty)

        case Failure(t) => throw t
      }

    } finally {
      GatlingActorSystem.shutdown()
    }
}
