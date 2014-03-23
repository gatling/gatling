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
package io.gatling.core.controller.throttle

import java.lang.System.nanoTime

import scala.concurrent.duration.DurationInt

import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.config.Protocol
import io.gatling.core.util.TimeHelper.secondsSinceReference

case class ThrottlingProtocol(limit: Long => Int) extends Protocol

class ThisSecondThrottler(val limit: Int, var count: Int = 0) {

  def increment() { count += 1 }
  def limitReached = count >= limit
}

class Throttler(globalProfile: Option[ThrottlingProtocol], scenarioProfiles: Map[String, ThrottlingProtocol]) extends AkkaDefaults {

  val buffer = collection.mutable.Queue.empty[(String, () => Unit)]

  var thisTickStartNanoRef: Long = _
  var thisTickGlobalThrottler: Option[ThisSecondThrottler] = _
  var thisTickPerScenarioThrottlers: Map[String, ThisSecondThrottler] = _
  var requestPeriod: Double = _
  var thisTickRequestCount: Int = _

  newSecond()

  private def newSecond() {
    thisTickStartNanoRef = nanoTime
    val thisTickStartSeconds = secondsSinceReference
    thisTickGlobalThrottler = globalProfile.map(p => new ThisSecondThrottler(p.limit(thisTickStartSeconds)))
    thisTickPerScenarioThrottlers = Map.empty ++ scenarioProfiles.mapValues(p => new ThisSecondThrottler(p.limit(thisTickStartSeconds)))
    val globalLimit = thisTickGlobalThrottler.map(_.limit)
    val perScenarioLimits = thisTickPerScenarioThrottlers.map(_._2.limit)
    val maxNumberOfRequests =
      if (perScenarioLimits.isEmpty)
        globalLimit.get
      else
        math.max(perScenarioLimits.max, globalLimit.getOrElse(0))

    requestPeriod = 1000.0 / maxNumberOfRequests
    thisTickRequestCount = 0
  }

  private def throttle(scenarioName: String, request: () => Unit, shiftInMillis: Int) = {
    val scenarioThrottler = thisTickPerScenarioThrottlers.get(scenarioName)

    val sending = !thisTickGlobalThrottler.exists(_.limitReached) && !scenarioThrottler.exists(_.limitReached)
    if (sending) {
      thisTickGlobalThrottler.foreach(_.increment)
      scenarioThrottler.foreach(_.increment)
      val delay = (requestPeriod * thisTickRequestCount).toInt - shiftInMillis
      thisTickRequestCount += 1

      if (delay > 0)
        scheduler.scheduleOnce(delay milliseconds) {
          request()
        }
      else
        request()
    }

    sending
  }

  def millisSinceTickStart: Int = ((nanoTime - thisTickStartNanoRef) / 1000000).toInt

  def send(scenarioName: String, request: () => Unit) {
    if (!throttle(scenarioName, request, millisSinceTickStart))
      buffer += scenarioName -> request
  }

  def flushBuffer() {
    newSecond()
    // FIXME ugly, side effecting, can do better?
    // + no need to keep on testing when global limit is reached
    buffer.dequeueAll { case (scenarioName, request) => throttle(scenarioName, request, 0) }
  }
}
