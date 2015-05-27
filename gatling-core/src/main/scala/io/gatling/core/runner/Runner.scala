/**
 * Copyright 2011-2015 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.runner

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{ Try, Failure, Success }

import io.gatling.core.stats.StatsEngineFactory
import io.gatling.core.stats.writer.RunMessage

import akka.actor.ActorSystem
import akka.pattern.ask
import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.{ Run, Controller }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.funspec.GatlingFunSpec
import io.gatling.core.util.TimeHelper._
import com.typesafe.scalalogging.StrictLogging

case class RunResult(runId: String, hasAssertions: Boolean)

class Runner(selection: Selection)(implicit configuration: GatlingConfiguration) extends StrictLogging {

  def run: RunResult = {

    // start actor system before creating simulation instance, some components might need it (e.g. shutdown hook)
    val system = ActorSystem("GatlingSystem")

    try {
      val simulationClass = selection.simulationClass
      println(s"Simulation ${simulationClass.getName} started...")

      // important, initialize time reference
      val timeRef = NanoTimeReference

      // ugly way to pass the configuration to the Simulation constructor
      io.gatling.core.Predef.configuration = configuration

      val simulation = simulationClass.newInstance

      val simulationParams = simulation.params

      simulationParams.beforeSteps.foreach(_.apply())

      val runMessage = RunMessage(selection.simulationClass.getName, selection.simulationId, nowMillis, selection.description)

      val statsEngine = {
        val statsEngineFactory = Class.forName(configuration.data.statsEngineFactoryClass).newInstance().asInstanceOf[StatsEngineFactory]
        val statsEngineF = statsEngineFactory.apply(system, simulationParams.populationBuilders, simulationParams.assertions, selection, runMessage)
        Await.result(statsEngineF, 5 seconds).get
      }

      val controller = system.actorOf(Controller.props(selection, statsEngine, configuration), Controller.ControllerActorName)
      val throttler = Throttler(system, simulationParams)
      val exit = system.actorOf(Exit.props(controller), Exit.ExitActorName)

      val coreComponents = CoreComponents(controller, throttler, statsEngine, exit)

      val scenarios = simulationParams.scenarios(system, coreComponents)

      System.gc()

      val timeout = Int.MaxValue.milliseconds - 10.seconds

      val runResult = controller.ask(Run(scenarios, simulationParams))(timeout).mapTo[Try[String]]

      val res = Await.result(runResult, timeout)

      res match {
        case Success(_) =>
          println("Simulation finished")
          simulationParams.afterSteps.foreach(_.apply())
          RunResult(runMessage.runId, simulationParams.assertions.nonEmpty)

        case Failure(t) => throw t
      }

    } finally {
      system.shutdown()
      system.awaitTermination()
    }
  }
}
