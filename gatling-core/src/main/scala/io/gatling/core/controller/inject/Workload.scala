/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

import scala.concurrent.duration.{ Duration, FiniteDuration }

import io.gatling.commons.util.Clock
import io.gatling.core.scenario.Scenario
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.stats.writer.UserEndMessage

import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ EventLoop, EventLoopGroup }

abstract class Workload(
    scenario: Scenario,
    userIdGen: AtomicLong,
    eventLoopGroup: EventLoopGroup,
    statsEngine: StatsEngine,
    clock: Clock
) extends StrictLogging {

  private var scheduled = 0
  private var stopped = 0
  private var allScheduled = false

  private def incrementScheduledUsers(): Unit = scheduled += 1

  protected def setAllScheduled(): Unit = allScheduled = true

  protected def incrementStoppedUsers(): Unit = stopped += 1

  private def startUser(userId: Long, eventLoop: EventLoop): Unit = {
    val rawSession = Session(scenario.name, userId, scenario.onExit, eventLoop)
    val session = scenario.onStart(rawSession)
    val timestamp = clock.nowMillis
    scenario.entry ! session
    logger.debug(s"Start user #${session.userId}")
    statsEngine.logUserStart(scenario.name, timestamp)
  }

  protected def injectUser(delay: FiniteDuration): Unit = {
    incrementScheduledUsers()
    val userId = userIdGen.incrementAndGet()
    val eventLoop = eventLoopGroup.next()
    if (!eventLoop.isShutdown) {
      if (delay <= Duration.Zero) {
        eventLoop.execute(() => startUser(userId, eventLoop))
      } else {
        eventLoop.schedule((() => startUser(userId, eventLoop)): Runnable, delay.toMillis, TimeUnit.MILLISECONDS)
      }
    }
  }

  protected def getConcurrentUsers: Int = scheduled - stopped

  def scenarioName: String = scenario.name

  def injectBatch(batchWindow: FiniteDuration): Unit

  def endUser(userMessage: UserEndMessage): Unit

  def isAllUsersScheduled: Boolean = allScheduled

  def isAllUsersStopped: Boolean = allScheduled && scheduled == stopped

  def duration: FiniteDuration

  def isEmpty: Boolean
}
