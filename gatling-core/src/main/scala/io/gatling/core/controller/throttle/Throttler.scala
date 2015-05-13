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
package io.gatling.core.controller.throttle

import java.lang.System.nanoTime

import io.gatling.core.scenario.SimulationDef

import scala.concurrent.duration.{ FiniteDuration, DurationInt }

import akka.actor.{ Props, ActorSystem, ActorRef }
import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.akka.BaseActor

sealed trait ThrottlerMessage
case object OneSecondTick extends ThrottlerMessage
case class ThrottledRequest(scenarioName: String, request: () => Unit) extends ThrottlerMessage

case class ThrottlingProfile(limit: Long => Int, duration: FiniteDuration)

class ThisSecondThrottle(val limit: Int, var count: Int = 0) {

  def increment(): Unit = count += 1
  def limitReached: Boolean = count >= limit
}

object Throttler {
  def apply(system: ActorSystem, simulationDef: SimulationDef, throttlerActorName: String) =
    new Throttler(system.actorOf(ThrottlerActor.props(simulationDef), throttlerActorName))
}

class Throttler(throttlerActor: ActorRef) {

  def throttle(scenarioName: String, action: () => Unit): Unit =
    throttlerActor ! ThrottledRequest(scenarioName, action)
}

object ThrottlerActor extends StrictLogging {

  def props(simulationDef: SimulationDef) =
    Props(new ThrottlerActor(simulationDef.globalThrottling, simulationDef.scenarioThrottlings))
}

class ThrottlerActor(globalProfile: Option[ThrottlingProfile], scenarioProfiles: Map[String, ThrottlingProfile]) extends BaseActor {

  val timerCancellable = system.scheduler.schedule(0 seconds, 1 seconds, self, OneSecondTick)

  override def postStop(): Unit = timerCancellable.cancel()

  // FIXME use a capped size?
  val buffer = collection.mutable.Queue.empty[(String, () => Unit)]

  var thisTickStartNanoRef: Long = _
  var thisTickGlobalThrottle: Option[ThisSecondThrottle] = _
  var thisTickPerScenarioThrottles: Map[String, ThisSecondThrottle] = _
  var requestPeriod: Double = _
  var thisTickRequestCount: Int = _

  var thisTickStartSeconds: Int = -1

  newSecond()

  private def newSecond(): Unit = {
    thisTickStartNanoRef = nanoTime

    if (thisTickStartSeconds != 0 || thisTickRequestCount > 0) {
      // either uninitialized
      // or has indeed started
      tick()
    }
  }

  private def tick(): Unit = {
    thisTickStartSeconds += 1
    thisTickGlobalThrottle = globalProfile.map(p => new ThisSecondThrottle(p.limit(thisTickStartSeconds)))
    thisTickPerScenarioThrottles = Map.empty ++ scenarioProfiles.mapValues(p => new ThisSecondThrottle(p.limit(thisTickStartSeconds)))
    val globalLimit = thisTickGlobalThrottle.map(_.limit)
    val perScenarioLimit =
      if (thisTickPerScenarioThrottles.nonEmpty)
        Some(thisTickPerScenarioThrottles.map(_._2.limit).sum)
      else
        None

    val maxNumberOfRequests = math.min(perScenarioLimit.getOrElse(Int.MaxValue), globalLimit.getOrElse(Int.MaxValue))

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
