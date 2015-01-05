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

import scala.concurrent.duration.{ FiniteDuration, DurationInt }

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.akka.BaseActor
import io.gatling.core.controller.Controller._
import io.gatling.core.util.TimeHelper.secondsSinceReference

sealed trait ThrottlerMessage
case object OneSecondTick extends ThrottlerMessage
case class ThrottledRequest(scenarioName: String, request: () => Unit) extends ThrottlerMessage

case class ThrottlingProfile(limit: Long => Int, duration: FiniteDuration)

class ThisSecondThrottle(val limit: Int, var count: Int = 0) {

  def increment(): Unit = count += 1
  def limitReached: Boolean = count >= limit
}

object Throttler extends StrictLogging {

  private var _instance: Option[ActorRef] = None

  def start(globalProfile: Option[ThrottlingProfile], scenarioProfiles: Map[String, ThrottlingProfile]): Unit = {

    val throttler = actor("controller")(new Throttler(globalProfile, scenarioProfiles))

    _instance = Some(throttler)
    logger.debug("Setting up throttling")
    scheduler.schedule(0 seconds, 1 seconds, throttler, OneSecondTick)
    system.registerOnTermination(_instance = None)
  }

  def throttle(scenarioName: String, action: () => Unit): Unit =
    _instance match {
      case Some(t) => t ! ThrottledRequest(scenarioName, action)
      case None    => logger.debug("Throttler hasn't been started")
    }
}

class Throttler(globalProfile: Option[ThrottlingProfile], scenarioProfiles: Map[String, ThrottlingProfile]) extends BaseActor {

  val buffer = collection.mutable.Queue.empty[(String, () => Unit)]

  var thisTickStartNanoRef: Long = _
  var thisTickGlobalThrottle: Option[ThisSecondThrottle] = _
  var thisTickPerScenarioThrottles: Map[String, ThisSecondThrottle] = _
  var requestPeriod: Double = _
  var thisTickRequestCount: Int = _

  newSecond()

  private def newSecond(): Unit = {
    thisTickStartNanoRef = nanoTime
    val thisTickStartSeconds = secondsSinceReference
    thisTickGlobalThrottle = globalProfile.map(p => new ThisSecondThrottle(p.limit(thisTickStartSeconds)))
    thisTickPerScenarioThrottles = Map.empty ++ scenarioProfiles.mapValues(p => new ThisSecondThrottle(p.limit(thisTickStartSeconds)))
    val globalLimit = thisTickGlobalThrottle.map(_.limit)
    val perScenarioLimits = thisTickPerScenarioThrottles.map(_._2.limit)
    val maxNumberOfRequests =
      if (perScenarioLimits.isEmpty)
        globalLimit.get
      else
        math.max(perScenarioLimits.max, globalLimit.getOrElse(0))

    requestPeriod = 1000.0 / maxNumberOfRequests
    thisTickRequestCount = 0
  }

  private def throttle(scenarioName: String, request: () => Unit, shiftInMillis: Int) = {
    val scenarioThrottler = thisTickPerScenarioThrottles.get(scenarioName)

    val sending = !thisTickGlobalThrottle.exists(_.limitReached) && !scenarioThrottler.exists(_.limitReached)
    if (sending) {
      thisTickGlobalThrottle.foreach(_.increment())
      scenarioThrottler.foreach(_.increment())
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

  private def flushBuffer(): Unit = {
    newSecond()
    // FIXME ugly, side effecting, can do better?
    // + no need to keep on testing when global limit is reached
    buffer.dequeueAll { case (scenarioName, request) => throttle(scenarioName, request, 0) }
  }

  private def send(scenarioName: String, request: () => Unit): Unit =
    if (!throttle(scenarioName, request, millisSinceTickStart))
      buffer += scenarioName -> request

  def receive = {
    case OneSecondTick                           => flushBuffer()
    case ThrottledRequest(scenarioName, request) => send(scenarioName, request)
  }
}
