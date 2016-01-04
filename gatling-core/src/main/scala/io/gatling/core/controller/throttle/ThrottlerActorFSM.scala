/**
 * Copyright 2011-2016 GatlingCorp (http://gatling.io)
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

import io.gatling.commons.util.Collections._
import io.gatling.core.akka.BaseActor

import akka.actor.FSM

private[throttle] object ThrottlerActorState {
  private[throttle] case object WaitingToStart extends ThrottlerActorState
  private[throttle] case object Started extends ThrottlerActorState
}
private[throttle] sealed trait ThrottlerActorState

private[throttle] object ThrottlerActorData {
  private[throttle] case object NoData extends ThrottlerActorData

  // mutable state is very ugly and error prone, but we're trying to limit allocations...
  private[throttle] case class StartedData(throttles: Throttles, buffer: mutable.ArrayBuffer[ThrottledRequest], tickNanos: Long) extends ThrottlerActorData {

    var count: Int = 0

    def incrementCount(): Unit = count += 1

    val requestStep = {

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
private[throttle] sealed trait ThrottlerActorData

private[throttle] class ThrottlerActorFSM extends BaseActor with FSM[ThrottlerActorState, ThrottlerActorData]
