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
package io.gatling.core.controller

import io.gatling.core.scenario.Scenario
import io.gatling.core.akka.BaseActor

import akka.actor.{ ActorRef, FSM }

private[controller] trait ControllerFSM extends BaseActor with FSM[ControllerState, ControllerData]

private[controller] sealed trait ControllerState
private[controller] object ControllerState {
  case object WaitingToStart extends ControllerState
  case object Started extends ControllerState
  case object WaitingForResourcesToStop extends ControllerState
  case object Stopped extends ControllerState
}

private[controller] class UserCounts(var completed: Long, var expected: Long) {
  def allStopped: Boolean = expected > 0 && completed == expected
}

private[controller] sealed trait ControllerData
private[controller] object ControllerData {
  case object NoData extends ControllerData
  case class InitData(launcher: ActorRef, scenarios: List[Scenario])
  case class StartedData(initData: InitData, userCounts: UserCounts) extends ControllerData
  case class EndData(initData: InitData, exception: Option[Throwable]) extends ControllerData
}

sealed trait ControllerCommand
object ControllerCommand {
  case class Start(scenarios: List[Scenario]) extends ControllerCommand
  case class InjectionStopped(count: Long) extends ControllerCommand
  case class ForceStop(e: Option[Throwable] = None) extends ControllerCommand
  case object StatsEngineStopped extends ControllerCommand
}
