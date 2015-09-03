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

import scala.collection.mutable

import io.gatling.core.akka.BaseActor

import akka.actor.FSM

private[throttle] object ThrottlerState {
  private[throttle] case object WaitingToStart extends ThrottlerState
  private[throttle] case object Started extends ThrottlerState
}
private[throttle] sealed trait ThrottlerState

private[throttle] object ThrottlerData {
  private[throttle] case object NoData extends ThrottlerData

  // mutable state is very ugly and error prone, but we're trying to limit allocations...
  private[throttle] case class StartedData(throttles: Throttles, buffer: mutable.ArrayBuffer[(String, () => Unit)], tickNanos: Long) extends ThrottlerData {

    var count: Int = 0

    def incrementCount(): Unit = count += 1

    val requestStep = {

      val globalLimit = throttles.global.map(_.limit).getOrElse(Int.MaxValue)
      val perScenarioLimit =
        if (throttles.perScenario.isEmpty)
          Int.MaxValue
        else
          throttles.perScenario.values.map(_.limit).sum
      val limit = math.min(globalLimit, perScenarioLimit)

      1000.0 / limit
    }
  }
}
private[throttle] sealed trait ThrottlerData

class ThrottlerFSM extends BaseActor with FSM[ThrottlerState, ThrottlerData]
