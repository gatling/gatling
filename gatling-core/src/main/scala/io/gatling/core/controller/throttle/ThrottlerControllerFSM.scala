/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.core.akka.BaseActor

import akka.actor.FSM

private[throttle] sealed trait ThrottlerControllerState
private[throttle] object ThrottlerControllerState {
  case object WaitingToStart extends ThrottlerControllerState
  case object Started extends ThrottlerControllerState
  case object Overridden extends ThrottlerControllerState
}

private[throttle] sealed trait ThrottlerControllerData
private[throttle] object ThrottlerControllerData {
  case object NoData extends ThrottlerControllerData
  final case class StartedData(tick: Int) extends ThrottlerControllerData
  final case class OverrideData(overrides: Throttlings, tick: Int) extends ThrottlerControllerData
}

private[throttle] class ThrottlerControllerFSM extends BaseActor with FSM[ThrottlerControllerState, ThrottlerControllerData]
