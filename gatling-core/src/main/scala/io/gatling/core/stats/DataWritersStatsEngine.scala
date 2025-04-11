/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.core.stats

import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

import io.gatling.commons.stats.Status
import io.gatling.commons.util.Clock
import io.gatling.core.actor.{ ActorRef, ActorSystem }
import io.gatling.core.config.GatlingConfiguration
import io.gatling.core.controller.Controller
import io.gatling.core.scenario.SimulationParams
import io.gatling.core.session.GroupBlock
import io.gatling.core.stats.writer._
import io.gatling.core.structure.PopulationBuilder

object DataWritersStatsEngine {
  def apply(
      simulationParams: SimulationParams,
      runMessage: RunMessage,
      system: ActorSystem,
      clock: Clock,
      resultsDirectory: Option[Path],
      configuration: GatlingConfiguration
  ): DataWritersStatsEngine = {
    val allPopulationBuilders = PopulationBuilder.flatten(simulationParams.rootPopulationBuilders)
    val scenarios = allPopulationBuilders.map(pb => ShortScenarioDescription(pb.scenarioBuilder.name, pb.injectionProfile.totalUserCount))

    val dataWriters = configuration.data.dataWriters
      .map {
        case DataWriterType.Console => new ConsoleDataWriter(runMessage, scenarios, clock, configuration.data.console)
        case DataWriterType.File =>
          LogFileDataWriter(
            runMessage,
            scenarios,
            simulationParams.assertions,
            resultsDirectory.getOrElse(throw new IllegalArgumentException("Can't use the file DataWriter without setting the results directory"))
          )
      }
      .map(system.actorOf)

    new DataWritersStatsEngine(
      dataWriters,
      system,
      clock
    )
  }
}

final class DataWritersStatsEngine(
    dataWriters: Seq[ActorRef[DataWriterMessage]],
    system: ActorSystem,
    clock: Clock
) extends StatsEngine {
  private val active = new AtomicBoolean(true)

  override def start(): Unit = dataWriters.foreach(_ ! DataWriterMessage.Init)

  override def stop(controller: ActorRef[Controller.Command], exception: Option[Exception]): Unit =
    if (active.getAndSet(false)) {
      val responses = dataWriters.map { dataWriter =>
        val promise = dataWriter.replyPromise[Unit](5.seconds)
        dataWriter ! DataWriterMessage.Stop(promise)
        promise.future
      }
      implicit val executionContext: ExecutionContext = system.executionContext
      Future.sequence(responses).onComplete(_ => controller ! Controller.Command.StatsEngineStopped)
    }

  private def dispatch(message: DataWriterMessage): Unit = if (active.get) dataWriters.foreach(_ ! message)

  override def logUserStart(scenario: String): Unit = dispatch(DataWriterMessage.LoadEvent.User(scenario, clock.nowMillis, start = true))

  override def logUserEnd(scenario: String): Unit = dispatch(DataWriterMessage.LoadEvent.User(scenario, clock.nowMillis, start = false))

  override def logResponse(
      scenario: String,
      groups: List[String],
      requestName: String,
      startTimestamp: Long,
      endTimestamp: Long,
      status: Status,
      responseCode: Option[String],
      message: Option[String]
  ): Unit =
    if (endTimestamp >= 0) {
      dispatch(
        DataWriterMessage.LoadEvent.Response(
          scenario,
          groups,
          requestName,
          startTimestamp,
          endTimestamp,
          status,
          responseCode,
          message
        )
      )
    }

  override def logGroupEnd(
      scenario: String,
      groupBlock: GroupBlock,
      exitTimestamp: Long
  ): Unit =
    dispatch(
      DataWriterMessage.LoadEvent.Group(
        scenario,
        groupBlock.groups,
        groupBlock.startTimestamp,
        exitTimestamp,
        groupBlock.cumulatedResponseTime,
        groupBlock.status
      )
    )

  override def logRequestCrash(scenario: String, groups: List[String], requestName: String, error: String): Unit =
    dispatch(DataWriterMessage.LoadEvent.Error(s"$requestName: $error ", clock.nowMillis))
}
