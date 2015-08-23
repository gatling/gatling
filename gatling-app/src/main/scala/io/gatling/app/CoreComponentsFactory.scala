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
package io.gatling.app

import scala.concurrent.{ Future, Await }
import scala.concurrent.duration._
import scala.util.{ Failure, Success }

import io.gatling.core.CoreComponents
import io.gatling.core.action.Exit
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.Controller
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.stats.{ DefaultStatsEngine, StatsEngine }
import io.gatling.core.stats.writer.{ Init, ShortScenarioDescription, RunMessage }
import io.gatling.core.util.ReflectionHelper._

import akka.actor.{ Props, Actor, ActorSystem }
import akka.pattern._
import akka.util.Timeout

private[gatling] object CoreComponentsFactory {

  val CoreComponentsFactorySystemProperty = "gatling.coreComponentsFactory"

  def apply(configuration: GatlingConfiguration): CoreComponentsFactory =
    sys.props.get(CoreComponentsFactorySystemProperty).map(newInstance[CoreComponentsFactory])
      .getOrElse(new DefaultCoreComponentsFactory)
}

private[gatling] trait CoreComponentsFactory {

  def coreComponents(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage)(implicit configuration: GatlingConfiguration): CoreComponents

  def resultsProcessor(implicit configuration: GatlingConfiguration): ResultsProcessor
}

private[gatling] class DefaultCoreComponentsFactory extends CoreComponentsFactory {

  private def newStatsEngine(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage)(implicit configuration: GatlingConfiguration): StatsEngine = {

    implicit val dataWriterTimeOut = Timeout(5 seconds)

    val dataWriters = configuration.data.dataWriters.map { dw =>
      val clazz = Class.forName(dw.className).asInstanceOf[Class[Actor]]
      system.actorOf(Props(clazz), clazz.getName)
    }

    val shortScenarioDescriptions = simulationParams.populationBuilders.map(pb => ShortScenarioDescription(pb.scenarioBuilder.name, pb.injectionProfile.userCount))

    val responses = dataWriters.map(_ ? Init(configuration, simulationParams.assertions, runMessage, shortScenarioDescriptions))

      def allSucceeded(responses: Seq[Any]): Boolean =
        responses.map {
          case b: Boolean => b
          case _          => false
        }.forall(identity)

    implicit val dispatcher = system.dispatcher

    val statsEngineF = Future.sequence(responses)
      .map(allSucceeded)
      .map {
        case true  => Success(new DefaultStatsEngine(system, dataWriters))
        case false => Failure(new Exception("DataWriters didn't initialize properly"))
      }

    Await.result(statsEngineF, 5 seconds).get
  }

  def coreComponents(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage)(implicit configuration: GatlingConfiguration): CoreComponents = {
    val statsEngine = newStatsEngine(system, simulationParams, runMessage)

    val throttler = Throttler(system, simulationParams)
    val controller = system.actorOf(Controller.props(statsEngine, throttler, simulationParams, configuration), Controller.ControllerActorName)
    val exit = system.actorOf(Exit.props(controller, statsEngine), Exit.ExitActorName)

    CoreComponents(controller, throttler, statsEngine, exit)
  }

  def resultsProcessor(implicit configuration: GatlingConfiguration): ResultsProcessor = new DefaultResultsProcessor()
}
