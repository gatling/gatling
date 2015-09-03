/**
 * Copyright 2011-2015 GatlingCorp (http://gatling.io)
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

import scala.concurrent.duration._

import akka.actor.ActorRef

sealed trait ThrottlerControllerCommand
case object ThrottlerControllerStart extends ThrottlerControllerCommand
case class ThrottlerControllerOverrideStart(overrides: Throttlings) extends ThrottlerControllerCommand
case object ThrottlerControllerOverrideStop extends ThrottlerControllerCommand
case object ThrottlerControllerTick extends ThrottlerControllerCommand

class ThrottlerController(throttler: ActorRef, defaults: Throttlings) extends ThrottlerControllerFSM {

  import ThrottlerControllerState._
  import ThrottlerControllerData._

  def notifyThrottler(throttlings: Throttlings, tick: Int): Unit = {

    val throttles = Throttles(
      global = throttlings.global.map(p => new Throttle(p.limit(tick))),
      perScenario = throttlings.perScenario.mapValues(p => new Throttle(p.limit(tick)))
    )

    throttler ! throttles
  }

  startWith(WaitingToStart, NoData)

  when(WaitingToStart) {

    case Event(ThrottlerControllerStart, NoData) =>
      system.scheduler.schedule(Duration.Zero, 1 second, self, ThrottlerControllerTick)
      notifyThrottler(defaults, 0)
      goto(Started) using StartedData(0)
  }

  when(Started) {

    case Event(ThrottlerControllerTick, StartedData(tick)) =>
      notifyThrottler(defaults, tick)
      stay() using StartedData(tick + 1)

    case Event(ThrottlerControllerOverrideStart(overrides), StartedData(tick)) =>
      goto(Overridden) using OverrideData(overrides, tick)
  }

  when(Overridden) {

    case Event(ThrottlerControllerTick, OverrideData(overrides, tick)) =>
      notifyThrottler(overrides, tick)
      stay() using OverrideData(overrides, tick + 1)

    case Event(ThrottlerControllerOverrideStop, OverrideData(_, tick)) =>
      goto(Started) using StartedData(tick)
  }
}
