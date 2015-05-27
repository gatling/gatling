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

import akka.actor.{ Props, Actor, ActorSystem }
import akka.pattern._
import akka.util.Timeout

object CoreComponentsFactory {

  val CoreComponentsFactorySystemProperty = "gatling.coreComponentsFactory"

  def apply(configuration: GatlingConfiguration): CoreComponentsFactory =
    sys.props.get(CoreComponentsFactorySystemProperty).map(Class.forName(_).newInstance.asInstanceOf[CoreComponentsFactory])
      .getOrElse(new DefaultCoreComponentsFactory)
}

trait CoreComponentsFactory {

  private[gatling] def coreComponents(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage)(implicit configuration: GatlingConfiguration): CoreComponents

  private[gatling] def resultsProcessor(implicit configuration: GatlingConfiguration): ResultsProcessor
}

class DefaultCoreComponentsFactory extends CoreComponentsFactory {

  private[gatling] def statsEngine(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage)(implicit configuration: GatlingConfiguration): StatsEngine = {

    implicit val dataWriterTimeOut = Timeout(5 seconds)

    val dataWriters = configuration.data.dataWriterClasses.map { className =>
      val clazz = Class.forName(className).asInstanceOf[Class[Actor]]
      system.actorOf(Props(clazz), clazz.getName)
    }

    val shortScenarioDescriptions = simulationParams.populationBuilders.map(pb => ShortScenarioDescription(pb.scenarioBuilder.name, pb.injectionProfile.totalUserEstimate))

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

  private[gatling] def coreComponents(system: ActorSystem, simulationParams: SimulationParams, runMessage: RunMessage)(implicit configuration: GatlingConfiguration): CoreComponents = {
    val theStatsEngine = statsEngine(system, simulationParams, runMessage)

    val controller = system.actorOf(Controller.props(theStatsEngine, configuration), Controller.ControllerActorName)
    val throttler = Throttler(system, simulationParams)
    val exit = system.actorOf(Exit.props(controller), Exit.ExitActorName)

    CoreComponents(controller, throttler, theStatsEngine, exit)
  }

  private[gatling] def resultsProcessor(implicit configuration: GatlingConfiguration): ResultsProcessor = new DefaultResultsProcessor()
}
