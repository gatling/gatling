/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.Throwables._
import io.gatling.core.actor.{ Actor, ActorRef, Behavior, Cancellable, Effect }
import io.gatling.core.controller.inject.{ Injector, PopulationFlows }
import io.gatling.core.controller.throttle.Throttler
import io.gatling.core.scenario.{ Population, SimulationParams }
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
    final case class Init(populationFlows: PopulationFlows[String, Population], maxDurationTimer: Option[Cancellable], runDonePromise: Promise[Unit])
    final case class End(initData: Init, reason: Command.StopLoadGenerator.Reason)
  }

  private[gatling] sealed trait Command
  private[gatling] object Command {
    private[gatling] final case class Start(populationFlows: PopulationFlows[String, Population], runDonePromise: Promise[Unit]) extends Command
    private[gatling] object StopLoadGenerator {
      sealed trait Reason {
        def message: String
      }
      object Reason {
        final case class Stop private[Reason] (message: String) extends Reason
        final case class Crash private[Reason] (message: String, exception: Throwable) extends Reason

        val RunTerminated: Reason = Stop("Run completed successfully")
        def userForcedStop(userMessage: String): Reason = Stop(s"User forced run to stop: $userMessage")
        def userForcedCrash(userMessage: String): Reason = Stop(s"User forced run to crash: $userMessage")
        def maxDurationReached(duration: FiniteDuration): Reason = Stop(s"Max duration of $duration reached")
        def crash(e: Throwable): Reason = Crash(e.detailedMessage, e)
        def crash(message: String): Reason =
          Crash(
            message,
            new Exception(message) {
              override def fillInStackTrace(): Throwable = this
            }
          )
        // [ee]
        //
        // [ee]
      }
    }
    private[gatling] final case class StopLoadGenerator(reason: StopLoadGenerator.Reason) extends Command
    private[gatling] case object StatsEngineStopped extends Command
    // [e]
    //
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
    case Command.Start(populationFlows, runDonePromise) =>
      val maxDurationTimer = simulationParams.maxDuration.map { maxDuration =>
        logger.debug("Setting up max duration")
        scheduler.scheduleOnce(maxDuration) {
          self ! Command.StopLoadGenerator(Command.StopLoadGenerator.Reason.maxDurationReached(maxDuration))
        }
      }

      throttler.foreach(_ ! Throttler.Command.Start)
      statsEngine.start()
      injector ! Injector.Command.Start(self, populationFlows)
      become(started(Data.Init(populationFlows, maxDurationTimer, runDonePromise)))

    case msg => dieOnUnexpected(msg)
  }

  private def started(data: Data.Init): Behavior[Command] = {
    case Command.StopLoadGenerator(reason) =>
      data.maxDurationTimer.foreach(_.cancel())
      logger.info("Initiating graceful stop")
      val crash = reason match {
        case _: Command.StopLoadGenerator.Reason.Crash =>
          // already logged
          true
        case _ =>
          logger.info(reason.message)
          false
      }
      statsEngine.stop(self, crash)
      become(waitingForResourcesToStop(Data.End(data, reason)))

    // [e]
    //
    //
    //
    //
    //
    // [e]

    case msg => dropUnexpected(msg)
  }

  private def stop(endData: Data.End): Effect[Command] = {
    endData.reason match {
      case Controller.Command.StopLoadGenerator.Reason.Crash(_, exception) => endData.initData.runDonePromise.tryFailure(exception)
      case _                                                               => endData.initData.runDonePromise.trySuccess(())
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
