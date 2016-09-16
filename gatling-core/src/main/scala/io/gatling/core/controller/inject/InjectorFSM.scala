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
package io.gatling.core.controller.inject

import io.gatling.core.akka.BaseActor

import akka.actor.{ Cancellable, FSM }

private[inject] trait InjectorState
private[inject] object InjectorState {
  case object WaitingToStart extends InjectorState
  case object Started extends InjectorState
}

private[inject] trait InjectorData
private[inject] object InjectorData {
  case object NoData extends InjectorData
  case class StartedData(startMillis: Long, count: Long, timer: Cancellable) extends InjectorData
}

private[inject] class InjectorFSM extends BaseActor with FSM[InjectorState, InjectorData]
