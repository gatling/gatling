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

package io.gatling.core.controller

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.akka.BaseActor
import io.gatling.core.scenario.Scenarios

import akka.actor.{ ActorRef, FSM }

private[controller] trait ControllerFSM extends BaseActor with FSM[ControllerState, ControllerData]

private[controller] sealed trait ControllerState
private[controller] object ControllerState {
  case object WaitingToStart extends ControllerState
  case object Started extends ControllerState
  case object WaitingForResourcesToStop extends ControllerState
  case object Stopped extends ControllerState
}

private[controller] sealed trait ControllerData
private[controller] object ControllerData {
  case object NoData extends ControllerData
  final case class InitData(launcher: ActorRef, scenarios: Scenarios)
  final case class StartedData(initData: InitData) extends ControllerData
  final case class EndData(initData: InitData, exception: Option[Exception]) extends ControllerData
}

sealed trait ControllerCommand
object ControllerCommand {
  final case class Start(scenarios: Scenarios) extends ControllerCommand
  final case object InjectorStopped extends ControllerCommand
  final case class Crash(exception: Exception) extends ControllerCommand
  final case class MaxDurationReached(duration: FiniteDuration) extends ControllerCommand
  final case object Kill extends ControllerCommand
  final case object StatsEngineStopped extends ControllerCommand
}
