/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.core.controller

import scala.concurrent.Promise
import scala.concurrent.duration.FiniteDuration

import io.gatling.core.actor.{ Actor, ActorRef, Behavior, Cancellable, Effect }
import io.gatling.core.controller.inject.{ Injector, ScenarioFlows }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.{ Scenario, SimulationParams }
import io.gatling.core.stats.StatsEngine

private[gatling] object Controller {
  def actor(
      statsEngine: StatsEngine,
      injector: ActorRef[Injector.Command],
      throttler: Option[ActorRef[Throttler.Command]],
      simulationParams: SimulationParams
  ): Actor[Command] =
    new Controller(statsEngine, injector, throttler, simulationParams)

  private object Data {
    final case class Init(scenarioFlows: ScenarioFlows[String, Scenario], maxDurationTimer: Option[Cancellable], runDonePromise: Promise[Unit])
    final case class End(initData: Init, exception: Option[Exception])
  }

  private[gatling] sealed trait Command
  private[gatling] object Command {
    private[gatling] final case class Start(scenarioFlows: ScenarioFlows[String, Scenario], runDonePromise: Promise[Unit]) extends Command
    private[gatling] final case object RunTerminated extends Command
    private[gatling] final case class Crash(exception: Exception) extends Command
    private[gatling] final case class MaxDurationReached(duration: FiniteDuration) extends Command
    private[gatling] final case object StopLoadGenerator extends Command
    private[gatling] final case object StatsEngineStopped extends Command
    // [e]
    private[gatling] final case object Kill extends Command
    // [e]
  }
}

private final class Controller private (
    statsEngine: StatsEngine,
    injector: ActorRef[Injector.Command],
    throttler: Option[ActorRef[Throttler.Command]],
    simulationParams: SimulationParams
) extends Actor[Controller.Command]("controller") {
  import Controller._

  override def init(): Behavior[Controller.Command] = {
    case Command.Start(scenarioFlows, runDonePromise) =>
      val maxDurationTimer = simulationParams.maxDuration.map { maxDuration =>
        logger.debug("Setting up max duration")
        scheduler.scheduleOnce(maxDuration) {
          self ! Command.MaxDurationReached(maxDuration)
        }
      }

      throttler.foreach(_ ! Throttler.Command.Start)
      statsEngine.start()
      injector ! Injector.Command.Start(self, scenarioFlows)
      become(started(Data.Init(scenarioFlows, maxDurationTimer, runDonePromise)))

    case msg => dieOnUnexpected(msg)
  }

  private def started(data: Data.Init): Behavior[Command] = {
    case Command.RunTerminated =>
      logger.info("Injector has stopped, initiating graceful stop")
      data.maxDurationTimer.foreach(_.cancel())
      stopGracefully(data, None)

    case Command.MaxDurationReached(maxDuration) =>
      logger.info(s"Max duration of $maxDuration reached")
      stopGracefully(data, None)

    case Command.Crash(exception) =>
      logger.info("Simulation crashed", exception)
      data.maxDurationTimer.foreach(_.cancel())
      stopGracefully(data, Some(exception))

    case Command.StopLoadGenerator =>
      logger.info("Load Generator was forcefully stopped")
      data.maxDurationTimer.foreach(_.cancel())
      stopGracefully(data, None)

    // [e]
    //
    //
    //
    //
    // [e]

    case msg => dropUnexpected(msg)
  }

  private def stopGracefully(initData: Data.Init, exception: Option[Exception]): Effect[Command] = {
    statsEngine.stop(self, exception)
    become(waitingForResourcesToStop(Data.End(initData, exception)))
  }

  private def stop(endData: Data.End): Effect[Command] = {
    endData.exception match {
      case Some(exception) => endData.initData.runDonePromise.tryFailure(exception)
      case _               => endData.initData.runDonePromise.trySuccess(())
    }
    die
  }

  private def waitingForResourcesToStop(data: Data.End): Behavior[Command] = {
    case Command.StatsEngineStopped =>
      logger.debug("StatsEngine was stopped")
      stop(data)

    // [e]
    //
    //
    //
    // [e]

    case msg => dropUnexpected(msg)
  }
}
