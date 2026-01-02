/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import scala.collection.mutable
import scala.concurrent.duration._

import io.gatling.commons.util.Collections._
import io.gatling.core.actor._

object Throttler {

  def actor(throttlings: Throttlings): Option[Actor[Command]] =
    Option.when(!throttlings.isEmpty)(new Throttler(throttlings))

  sealed trait Command
  object Command {
    case object Start extends Command
    case object Tick extends Command
    final case class ThrottledRequest(scenarioName: String, request: () => Unit) extends Command
  }

  private final case class Throttles(global: Option[Throttle], perScenario: Map[String, Throttle]) {
    def limitReached(scenario: String): Boolean =
      global.exists(_.limitReached) || perScenario.collectFirst { case (`scenario`, throttle) => throttle.limitReached }.getOrElse(false)

    def increment(scenario: String): Unit = {
      global.foreach(_.increment())
      perScenario.get(scenario).foreach(_.increment())
    }
  }

  private final class Throttle(val limit: Int) {
    private var count: Int = 0

    def increment(): Unit = count += 1

    def limitReached: Boolean = count >= limit

    override def toString = s"Throttle(limit=$limit, count=$count)"
  }

  // mutable state is very ugly and error prone, but we're trying to limit allocations...
  private final case class StartedData(
      throttles: Throttles,
      buffer: mutable.ArrayBuffer[Command.ThrottledRequest],
      tick: Int,
      tickNanos: Long
  ) {
    var count: Int = 0

    def incrementCount(): Unit = count += 1

    val requestStep: Double = {
      val globalLimit = throttles.global.map(_.limit).getOrElse(Int.MaxValue)
      val perScenarioLimit =
        if (throttles.perScenario.isEmpty)
          Int.MaxValue
        else
          throttles.perScenario.values.sumBy(_.limit)
      val limit = math.min(globalLimit, perScenarioLimit)

      1000.0 / limit
    }
  }
}

private final class Throttler private (throttlings: Throttlings) extends Actor[Throttler.Command]("throttler") {

  import Throttler._
  import Throttler.Command._

  override def init(): Behavior[Command] = {
    case Start =>
      scheduler.scheduleAtFixedRate(1.second)(self ! Tick)
      val throttles = computeThrottles(0)
      become(started(StartedData(throttles, mutable.ArrayBuffer.empty[ThrottledRequest], 0, nanoTime())))

    case msg => dieOnUnexpected(msg)
  }

  private def computeThrottles(tick: Int): Throttles =
    Throttles(
      global = throttlings.global.map(p => new Throttle(p.limit(tick))),
      perScenario = throttlings.perScenario.view.mapValues(p => new Throttle(p.limit(tick))).to(Map)
    )

  @SuppressWarnings(Array("org.wartremover.warts.Recursion"))
  private def started(data: StartedData): Behavior[Command] = {
    case throttledRequest: ThrottledRequest =>
      sendOrEnqueueRequest(data, throttledRequest)
      stay

    case Tick =>
      val newTick = data.tick + 1
      val newThrottles = computeThrottles(newTick)
      val newData = StartedData(newThrottles, new mutable.ArrayBuffer[ThrottledRequest](data.buffer.size), newTick, nanoTime())
      data.buffer.foreach(sendOrEnqueueRequest(newData, _))
      become(started(newData))

    case msg => dropUnexpected(msg)
  }

  private def sendOrEnqueueRequest(data: StartedData, throttledRequest: ThrottledRequest): Unit = {
    import data._
    if (throttles.limitReached(throttledRequest.scenarioName)) {
      buffer += throttledRequest
    } else {
      sendRequest(data, throttledRequest.request)
      throttles.increment(throttledRequest.scenarioName)
      data.incrementCount()
    }
  }

  private def sendRequest(data: StartedData, request: () => Unit): Unit = {
    import data._
    if (count == 0) {
      request()
    } else {
      val delay = ((requestStep * count).toInt - millisSinceTick(tickNanos)).milliseconds
      scheduler.scheduleOnce(delay) {
        request()
      }
    }
  }

  private def millisSinceTick(tickNanos: Long): Int = ((nanoTime - tickNanos) / 1000000).toInt
}
