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

import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration.{ Duration, FiniteDuration }

import io.gatling.core.scenario.Scenario
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.writer.UserMessage
import io.gatling.core.stats.message.Start

import akka.actor.ActorSystem
import com.typesafe.scalalogging.StrictLogging

abstract class Workload(
    scenario:    Scenario,
    userIdGen:   AtomicLong,
    startTime:   Long,
    system:      ActorSystem,
    statsEngine: StatsEngine
) extends StrictLogging {

  private var scheduled = 0
  private var stopped = 0
  private var allScheduled = false

  private def incrementScheduledUsers(): Unit = scheduled += 1

  protected def setAllScheduled(): Unit = allScheduled = true

  protected def incrementStoppedUsers(): Unit = stopped += 1

  private def startUser(userId: Long): Unit = {
    val rawSession = Session(scenario = scenario.name, userId = userId, onExit = scenario.onExit)
    val session = scenario.onStart(rawSession)
    scenario.entry ! session
    logger.debug(s"Start user #${session.userId}")
    statsEngine.logUser(UserMessage(session, Start, session.startDate))
  }

  protected def injectUser(delay: FiniteDuration): Unit = {
    incrementScheduledUsers()
    import system.dispatcher
    val userId = userIdGen.incrementAndGet()
    if (delay <= Duration.Zero) {
      startUser(userId)
    } else {
      system.scheduler.scheduleOnce(delay)(startUser(userId))
    }
  }

  protected def getConcurrentUsers: Int = scheduled - stopped

  def injectBatch(batchWindow: FiniteDuration): Unit

  def endUser(): Unit

  def isAllUsersScheduled: Boolean = allScheduled

  def isAllUsersStopped: Boolean = allScheduled && scheduled == stopped
}
