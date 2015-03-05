/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.result.writer

import java.lang.System.currentTimeMillis

import io.gatling.core.config.GatlingConfiguration

import scala.collection.mutable
import scala.concurrent.duration.DurationInt

import io.gatling.core.assertion.Assertion
import io.gatling.core.result.message.{ End, KO, OK, Start }

class UserCounters(val totalCount: Int) {

  private var _activeCount: Int = 0
  private var _doneCount: Int = 0

  def activeCount = _activeCount
  def doneCount = _doneCount

  def userStart(): Unit = { _activeCount += 1 }
  def userDone(): Unit = { _activeCount -= 1; _doneCount += 1 }
  def waitingCount = totalCount - _activeCount - _doneCount
}

class RequestCounters(var successfulCount: Int = 0, var failedCount: Int = 0)

class ConsoleDataWriter(implicit configuration: GatlingConfiguration) extends DataWriter with Flushable {

  private var startUpTime = 0L
  private var complete = false
  private val usersCounters = mutable.Map.empty[String, UserCounters]
  private val globalRequestCounters = new RequestCounters
  private val requestsCounters: mutable.Map[String, RequestCounters] = mutable.LinkedHashMap.empty
  private val errorsCounters: mutable.Map[String, Int] = mutable.LinkedHashMap.empty

  override def onInitializeDataWriter(assertions: Seq[Assertion], run: RunMessage, scenarios: Seq[ShortScenarioDescription]): Boolean = {

    startUpTime = currentTimeMillis

    scenarios.foreach(scenario => usersCounters.put(scenario.name, new UserCounters(scenario.nbUsers)))

    scheduler.schedule(0 seconds, 5 seconds, self, Flush)

    true
  }

  override def onFlush(): Unit = {
    val runDuration = (currentTimeMillis - startUpTime) / 1000

    val summary = ConsoleSummary(runDuration, usersCounters, globalRequestCounters, requestsCounters, errorsCounters)
    complete = summary.complete
    println(summary.text)
  }

  private def onUserMessage(userMessage: UserMessage): Unit = {

    import userMessage._

    event match {
      case Start =>
        usersCounters.get(scenario) match {
          case Some(name) => name.userStart()
          case _          => logger.error(s"Internal error, scenario '$scenario' has not been correctly initialized")
        }

      case End =>
        usersCounters.get(scenario) match {
          case Some(name) => name.userDone()
          case _          => logger.error(s"Internal error, scenario '$scenario' has not been correctly initialized")
        }
    }
  }

  private def onRequestMessage(request: RequestEndMessage): Unit = {

    import request._

    val requestPath = (groupHierarchy :+ name).mkString(" / ")
    val requestCounters = requestsCounters.getOrElseUpdate(requestPath, new RequestCounters)

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

  override def onMessage(message: LoadEventMessage): Unit = message match {
    case user: UserMessage          => onUserMessage(user)
    case request: RequestEndMessage => onRequestMessage(request)
    case _                          =>
  }

  override def onTerminateDataWriter(): Unit = if (!complete) onFlush()
}
