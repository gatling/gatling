/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.controller

import io.gatling.core.akka.BaseActor

import scala.concurrent.duration.FiniteDuration

import akka.actor.{ ActorRef, FSM }

import io.gatling.core.scenario.{ SimulationDef, Scenario }
import io.gatling.core.result.writer.UserMessage

private[controller] case class UserStream(
  scenario: Scenario,
  offset: Int,
  stream: Iterator[(FiniteDuration, Int)])

private[controller] trait ControllerFSM extends BaseActor with FSM[ControllerState, ControllerData]

private[controller] sealed trait ControllerState
private[controller] case object WaitingToStart extends ControllerState
private[controller] case object Running extends ControllerState
private[controller] case object WaitingForDataWritersToTerminate extends ControllerState
private[controller] case object Stopped extends ControllerState

private[controller] sealed trait ControllerData
private[controller] case object NoData extends ControllerData
private[controller] case class InitData(runner: ActorRef, simulationDef: SimulationDef)
private[controller] case class RunData(
  initData: InitData,
  userStreams: Map[String, UserStream],
  scheduler: BatchScheduler,
  activeUsers: Map[String, UserMessage],
  completedUsersCount: Int,
  totalUsers: Int) extends ControllerData
private[controller] case class EndData(
  initData: InitData,
  exception: Option[Exception]) extends ControllerData

sealed trait ControllerMessage
case class Run(simulation: SimulationDef) extends ControllerMessage
case class ForceTermination(e: Option[Exception] = None) extends ControllerMessage
case object DataWritersTerminated extends ControllerMessage
case class ScheduleNextUserBatch(scenarioName: String) extends ControllerMessage
