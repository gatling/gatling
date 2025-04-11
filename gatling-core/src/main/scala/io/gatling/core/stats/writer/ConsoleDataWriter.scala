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

package io.gatling.core.stats.writer

import java.time.{ Clock => JavaTimeClock, Instant, ZonedDateTime }
import java.time.format.DateTimeFormatter

import scala.collection.mutable

import io.gatling.commons.stats.{ KO, OK }
import io.gatling.commons.util.Clock
import io.gatling.core.actor.Cancellable
import io.gatling.core.config.ConsoleDataWriterConfiguration

private[writer] final class UserCounters(val totalUserCount: Option[Long]) {
  private var _activeCount: Long = 0
  private var _doneCount: Long = 0

  def activeCount: Long = _activeCount
  def doneCount: Long = _doneCount

  def userStart(): Unit = _activeCount += 1
  def userDone(): Unit = {
    _activeCount -= 1
    _doneCount += 1
  }
  def waitingCount: Long = totalUserCount.map(c => math.max(c - _activeCount - _doneCount, 0)).getOrElse(0L)
}

private object RequestCounters {
  def empty: RequestCounters = new RequestCounters(0, 0)
}

private[writer] final class RequestCounters(var successfulCount: Int, var failedCount: Int)

private[writer] final class ConsoleData(val startUpTime: Long, val dateTimeFormatter: DateTimeFormatter, val timer: Cancellable) extends DataWriterData {
  var complete: Boolean = false
  val usersCounters: mutable.Map[String, UserCounters] = mutable.Map.empty
  val globalRequestCounters: RequestCounters = RequestCounters.empty
  val requestsCounters: mutable.Map[String, RequestCounters] = mutable.LinkedHashMap.empty
  val errorsCounters: mutable.Map[String, Int] = mutable.LinkedHashMap.empty
}

private[gatling] final class ConsoleDataWriter(
    runMessage: RunMessage,
    scenarios: Seq[ShortScenarioDescription],
    clock: Clock,
    configuration: ConsoleDataWriterConfiguration
) extends DataWriter[ConsoleData]("console-data-writer") {

  override def onInit(): ConsoleData = {
    val timer = scheduler.scheduleAtFixedRate(configuration.writePeriod) {
      self ! DataWriterMessage.Flush
    }

    val data = new ConsoleData(clock.nowMillis, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss O").withZone(runMessage.zoneId), timer)

    scenarios.foreach(scenario => data.usersCounters.put(scenario.name, new UserCounters(scenario.totalUserCount)))

    data
  }

  override def onFlush(data: ConsoleData): Unit = {
    import data._

    val now = clock.nowMillis
    val runDuration = (now - startUpTime) / 1000

    val summary =
      ConsoleSummary(
        runDuration,
        usersCounters,
        globalRequestCounters,
        requestsCounters,
        errorsCounters,
        configuration.light,
        ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), JavaTimeClock.systemDefaultZone().getZone),
        dateTimeFormatter
      )
    complete = summary.complete
    println(summary.text)
  }

  override def onMessage(message: DataWriterMessage.LoadEvent, data: ConsoleData): Unit = message match {
    case user: DataWriterMessage.LoadEvent.User         => onUserMessage(user, data)
    case response: DataWriterMessage.LoadEvent.Response => onResponseMessage(response, data)
    case error: DataWriterMessage.LoadEvent.Error       => onErrorMessage(error, data)
    case _                                              =>
  }

  private def onUserMessage(user: DataWriterMessage.LoadEvent.User, data: ConsoleData): Unit = {
    import data._
    import user._

    usersCounters.get(scenario) match {
      case Some(userCounters) => if (user.start) userCounters.userStart() else userCounters.userDone()
      case _                  => logger.error(s"Internal error, scenario '$scenario' has not been correctly initialized")
    }
  }

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  private def onResponseMessage(response: DataWriterMessage.LoadEvent.Response, data: ConsoleData): Unit = {
    import data._
    import response._

    val requestPath = (groupHierarchy :+ response.name).mkString(" / ")
    val requestCounters = requestsCounters.getOrElseUpdate(requestPath, RequestCounters.empty)

    status match {
      case OK =>
        globalRequestCounters.successfulCount += 1
        requestCounters.successfulCount += 1
      case KO =>
        globalRequestCounters.failedCount += 1
        requestCounters.failedCount += 1
        val errorMessage = message.getOrElse("<no-message>")
        errorsCounters(errorMessage) = errorsCounters.getOrElse(errorMessage, 0) + 1
    }
  }

  private def onErrorMessage(error: DataWriterMessage.LoadEvent.Error, data: ConsoleData): Unit = {
    import data._
    errorsCounters(error.message) = errorsCounters.getOrElse(error.message, 0) + 1
  }

  override def onCrash(cause: String, data: ConsoleData): Unit = {}

  override def onStop(data: ConsoleData): Unit = {
    data.timer.cancel()
    if (!data.complete) onFlush(data)
  }
}
