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
package io.gatling.core.controller.inject

import scala.concurrent.duration._

import io.gatling.core.controller.UserStream
import io.gatling.core.scenario.Scenario
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.message.Start
import io.gatling.core.stats.writer.UserMessage
import io.gatling.core.util.TimeHelper._

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging

case class Injection(count: Long, continue: Boolean) {
  def +(other: Injection): Injection =
    Injection(count + other.count, continue && other.continue)
}

object Injector {

  def apply(system: ActorSystem, statsEngine: StatsEngine, batchWindow: FiniteDuration, scenarios: List[Scenario]): Injector = {
    val userStreams = scenarios.map(scenario => scenario.name -> UserStream(scenario, scenario.injectionProfile.allUsers)).toMap
    new DefaultInjector(system, statsEngine, batchWindow, userStreams)
  }
}

trait Injector {
  def inject(): Injection
  def batchWindow: FiniteDuration
}

// not thread-safe, supposed to be called only by controller with is an Actor and guarantees thread-safety
class DefaultInjector(system: ActorSystem, statsEngine: StatsEngine, val batchWindow: FiniteDuration, userStreams: Map[String, UserStream]) extends Injector with StrictLogging {

  var startTime: Long = _
  var userIdGen: Long = _

  private def initStartTime(): Unit =
    if (startTime == 0L) {
      startTime = nowMillis
    }

  private def newUserId(): Long = {
    userIdGen += 1
    userIdGen
  }

  def inject(): Injection = {
    initStartTime()
    userStreams.values.foldLeft(Injection(0, true)) {
      case (injection, userStream) =>
        injection + injectUserStream(userStream)
    }
  }

  private def injectUserStream(userStream: UserStream): Injection = {

    implicit val dispatcher = system.dispatcher

    val scenario = userStream.scenario
    val stream = userStream.stream

      def startUser(userId: Long): Unit = {
        val session = Session(scenario = scenario.name,
          userId = userId,
          onExit = scenario.onExit)
        scenario.entry ! session
        logger.info(s"Start user #${session.userId}")
        val userStart = UserMessage(session, Start, session.startDate)
        statsEngine.logUser(userStart)
      }

    if (stream.hasNext) {
      val batchTimeOffset = (nowMillis - startTime).millis
      val nextBatchTimeOffset = batchTimeOffset + batchWindow

      var continue = true
      var notLast = true
      var count = 0L

      while (notLast && continue) {

        val startingTime = stream.next()
        notLast = stream.hasNext
        val delay = startingTime - batchTimeOffset
        continue = startingTime < nextBatchTimeOffset
        count += 1
        val userId = newUserId()

        if (continue && delay <= ZeroMs) {
          startUser(userId)
        } else {
          // Reduce the starting time to the millisecond precision to avoid flooding the scheduler
          system.scheduler.scheduleOnce(toMillisPrecision(delay))(startUser(userId))
        }
      }

      Injection(count, notLast)
    } else {
      Injection(0, false)
    }
  }
}
