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
      sealed trait Reason extends Product with Serializable {
        def message: String
      }
      object Reason {
        sealed trait Graceful extends Reason
        object Graceful {
          case object Completed extends Graceful {
            override def message: String = "Run completed normally"
          }
          final case class Forced(userMessage: String) extends Graceful {
            override def message: String = s"Run stopped from code using stopLoadGenerator: $userMessage"
          }
          final case class MaxDurationReached(duration: FiniteDuration) extends Graceful {
            override def message: String = s"Run stopped on maxDuration($duration) reached"
          }
          // [e]
          //
          //
          //
          // [e]
        }
        sealed trait Crash extends Reason {
          def cause: Throwable
        }
        object Crash {
          final case class Forced(userMessage: String) extends Crash {
            override def message: String = s"Run crashed from code using crashLoadGenerator: $userMessage"
            override def cause: Throwable = new Exception(message) {
              override def fillInStackTrace(): Throwable = this
            }
          }
          final case class WellKnown(message: String) extends Crash {
            override def cause: Throwable = new Exception(message) {
              override def fillInStackTrace(): Throwable = this
            }
          }
          final case class Unexpected(cause: Throwable) extends Crash {
            override def message: String = cause.detailedMessage
          }
        }
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
          self ! Command.StopLoadGenerator(Command.StopLoadGenerator.Reason.Graceful.MaxDurationReached(maxDuration))
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
      case crash: Controller.Command.StopLoadGenerator.Reason.Crash => endData.initData.runDonePromise.tryFailure(crash.cause)
      case _                                                        => endData.initData.runDonePromise.trySuccess(())
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
