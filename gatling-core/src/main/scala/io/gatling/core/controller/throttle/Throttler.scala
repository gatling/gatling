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

import scala.concurrent.duration.{ Duration, DurationInt }

import akka.actor.{ Cancellable, Props, ActorSystem, ActorRef }
import com.typesafe.scalalogging.StrictLogging
import io.gatling.core.akka.BaseActor
import io.gatling.core.scenario.SimulationParams

sealed trait ThrottlerMessage
case object Start extends ThrottlerMessage
case object OneSecondTick extends ThrottlerMessage
case class ThrottledRequest(scenarioName: String, request: () => Unit) extends ThrottlerMessage

class ThisSecondThrottle(val limit: Int, var count: Int = 0) {

  def increment(): Unit = count += 1
  def limitReached: Boolean = count >= limit

  override def toString = s"ThisSecondThrottle(limit=$limit, count=$count)"
}

object Throttler {

  val ThrottlerActorName = "gatling-throttler"

  def apply(system: ActorSystem, simulationParams: SimulationParams) =
    new Throttler(system.actorOf(ThrottlerActor.props(simulationParams), ThrottlerActorName))
}

class Throttler(throttlerActor: ActorRef) {

  def start(): Unit = throttlerActor ! Start

  def throttle(scenarioName: String, action: () => Unit): Unit =
    throttlerActor ! ThrottledRequest(scenarioName, action)
}

object ThrottlerActor extends StrictLogging {
  def props(simulationParams: SimulationParams) =
    Props(new ThrottlerActor(simulationParams.globalThrottling, simulationParams.scenarioThrottlings))
}

class ThrottlerActor(globalThrottling: Option[Throttling], scenarioThrottlings: Map[String, Throttling]) extends BaseActor {

  // FIXME FSM
  var timerCancellable: Cancellable = _

  override def postStop(): Unit = timerCancellable.cancel()

  // FIXME use a capped size?
  val buffer = collection.mutable.Queue.empty[(String, () => Unit)]

  var thisTickStartNanoRef: Long = _
  var thisTickGlobalThrottle: Option[ThisSecondThrottle] = None
  var thisTickPerScenarioThrottles: Map[String, ThisSecondThrottle] = Map.empty
  var requestPeriod: Double = _
  var thisTickRequestCount: Int = _

  var thisTickStartSeconds: Int = -1

  private def start(): Unit = {
    timerCancellable = system.scheduler.schedule(Duration.Zero, 1 seconds, self, OneSecondTick)
    newSecond()
  }

  private def newSecond(): Unit = {
    thisTickStartNanoRef = nanoTime
    tick()
  }

  private def tick(): Unit = {
    thisTickStartSeconds += 1
    val last = thisTickGlobalThrottle
    thisTickGlobalThrottle = globalThrottling.map(p => new ThisSecondThrottle(p.limit(thisTickStartSeconds)))
    thisTickPerScenarioThrottles = scenarioThrottlings.mapValues(p => new ThisSecondThrottle(p.limit(thisTickStartSeconds)))
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
    case Start                                   => start()
    case OneSecondTick                           => flushBuffer()
    case ThrottledRequest(scenarioName, request) => send(scenarioName, request)
  }
}
