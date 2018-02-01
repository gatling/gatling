/*
 * Copyright 2011-2018 GatlingCorp (http://gatling.io)
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

package io.gatling.core.controller.inject

import scala.collection.breakOut
import scala.concurrent.duration._

import io.gatling.commons.util.{ LongCounter, PushbackIterator }
import io.gatling.commons.util.Collections._
import io.gatling.commons.util.ClockSingleton._
import io.gatling.core.controller.ControllerCommand.InjectorStopped
import io.gatling.core.scenario.Scenario
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.End
import io.gatling.core.stats.writer.UserMessage

import akka.actor.{ ActorRef, ActorSystem, Props }

sealed trait InjectorCommand
object InjectorCommand {
  case class Start(controller: ActorRef, scenarios: List[Scenario]) extends InjectorCommand
  case object Tick extends InjectorCommand
}

private[inject] case class UserStream(scenario: Scenario, stream: PushbackIterator[FiniteDuration]) {

  def withStream(batchWindow: FiniteDuration, injectTime: Long, startTime: Long)(f: (Scenario, FiniteDuration) => Unit): Injection = {

    if (stream.hasNext) {
      val batchTimeOffset = (injectTime - startTime).millis
      val nextBatchTimeOffset = batchTimeOffset + batchWindow

      var continue = true
      var streamNonEmpty = true
      var count = 0L

      while (streamNonEmpty && continue) {

        val startingTime = stream.next()
        streamNonEmpty = stream.hasNext
        val delay = startingTime - batchTimeOffset
        continue = startingTime < nextBatchTimeOffset

        if (continue) {
          count += 1
          // TODO instead of scheduling each user separately, we could group them by rounded-up delay (Akka defaults to 10ms)
          f(scenario, delay)
        } else {
          streamNonEmpty = true
          stream.pushback(startingTime)
        }
      }

      Injection(count, streamNonEmpty)
    } else {
      Injection.Empty
    }
  }
}

private[inject] object Injection {
  val Empty = Injection(0, continue = false)
}

private[inject] case class Injection(count: Long, continue: Boolean) {
  def +(other: Injection): Injection =
    Injection(count + other.count, continue && other.continue)
}

object Injector {

  private val InjectorActorName = "gatling-injector"
  val TickPeriod: FiniteDuration = 1 second
  val InitialBatchWindow: FiniteDuration = TickPeriod * 2

  def apply(system: ActorSystem, statsEngine: StatsEngine): ActorRef =
    system.actorOf(Props(new Injector(statsEngine)), InjectorActorName)
}

private[inject] class Injector(statsEngine: StatsEngine) extends InjectorFSM {

  import Injector._
  import InjectorState._
  import InjectorData._
  import InjectorCommand._

  val userIdGen = new LongCounter

  private def inject(data: StartedData, batchWindow: FiniteDuration): State = {
    import data._
    val injection = injectStreams(userStreams, batchWindow, startMillis)
    userCounts.incrementStarted(injection.count)

    if (injection.continue) {
      goto(Started) using data
    } else {
      logger.info(s"InjectionStopped expectedCount=${userCounts.started}")
      timer.cancel()
      goto(StoppedInjecting) using StoppedInjectingData(controller, userCounts)
    }
  }

  private def injectStreams(streams: Map[String, UserStream], batchWindow: FiniteDuration, startTime: Long): Injection = {
    val injections = streams.values.map(_.withStream(batchWindow, nowMillis, startTime)(injectUser))
    val totalCount = injections.sumBy(_.count)
    val totalContinue = injections.exists(_.continue)
    logger.debug(s"Injecting $totalCount users, continue=$totalContinue")
    Injection(totalCount, totalContinue)
  }

  private def startUser(scenario: Scenario, userId: Long): Unit = {
    val rawSession = Session(scenario = scenario.name, userId = userId, onExit = scenario.onExit)
    val session = scenario.onStart(rawSession)
    scenario.entry ! session
    logger.debug(s"Start user #${session.userId}")
    val userStart = UserMessage(session, io.gatling.core.stats.message.Start, session.startDate)
    statsEngine.logUser(userStart)
  }

  private def injectUser(scenario: Scenario, delay: FiniteDuration): Unit = {
    val userId = userIdGen.incrementAndGet()

    if (delay <= Duration.Zero) {
      startUser(scenario, userId)
    } else {
      system.scheduler.scheduleOnce(delay)(startUser(scenario, userId))
    }
  }

  startWith(WaitingToStart, NoData)

  when(WaitingToStart) {
    case Event(Start(controller, scenarios), NoData) =>
      val defaultStreams: Map[String, UserStream] = scenarios.map(scenario => scenario.name -> UserStream(scenario, new PushbackIterator(scenario.injectionProfile.allUsers)))(breakOut)
      val timer = system.scheduler.schedule(InitialBatchWindow, TickPeriod, self, Tick)
      inject(StartedData(controller, defaultStreams, nowMillis, timer, new UserCounts), InitialBatchWindow)
  }

  when(Started) {
    case Event(UserMessage(session, End, _), data: StartedData) =>
      import data._
      logger.debug(s"End user #${session.userId}")
      userCounts.incrementStopped()
      stay()

    case Event(Tick, data: StartedData) =>
      inject(data, TickPeriod)
  }

  when(StoppedInjecting) {
    case Event(UserMessage(_, End, _), StoppedInjectingData(controller, userCounts)) =>
      if (userCounts.allStopped) {
        logger.info("All users are stopped")
        controller ! InjectorStopped
        stop()
      } else {
        stay()
      }
  }
}
