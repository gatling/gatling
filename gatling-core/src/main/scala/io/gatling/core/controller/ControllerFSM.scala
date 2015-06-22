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
package io.gatling.core.controller

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.controller.inject.{ PushbackIterator, Injector }
import io.gatling.core.scenario.Scenario
import io.gatling.core.akka.BaseActor

import akka.actor.{ ActorRef, FSM }

private[controller] case class UserStream(scenario: Scenario, stream: PushbackIterator[FiniteDuration])

private[controller] trait ControllerFSM extends BaseActor with FSM[ControllerState, ControllerData]

private[controller] sealed trait ControllerState
private[controller] case object WaitingToStart extends ControllerState
private[controller] case object Started extends ControllerState
private[controller] case object WaitingForResourcesToStop extends ControllerState
private[controller] case object Stopped extends ControllerState

private[controller] sealed trait ControllerData
private[controller] case object NoData extends ControllerData
private[controller] case class InitData(launcher: ActorRef, scenarios: List[Scenario])
private[controller] class StartedData(
  val initData: InitData,
  val injector: Injector,
  var completedUsersCount: Long,
  var expectedUsersCount: Long,
  var injectionContinue: Boolean) extends ControllerData
private[controller] case class EndData(
  initData: InitData,
  exception: Option[Exception]) extends ControllerData

sealed trait ControllerMessage
case class Start(scenarios: List[Scenario]) extends ControllerMessage
case class ForceStop(e: Option[Exception] = None) extends ControllerMessage
case object StatsEngineStopped extends ControllerMessage
case object ScheduleNextInjection extends ControllerMessage
