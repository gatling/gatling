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
package io.gatling.metrics.sender

import akka.actor.{ ActorRef, FSM }

import io.gatling.core.util.Failures

private[sender] trait TcpSenderStateMachine extends FSM[TcpSenderState, TcpSenderData]

private[sender] sealed trait TcpSenderState
private[sender] case object WaitingForConnection extends TcpSenderState
private[sender] case object Running extends TcpSenderState
private[sender] case object RetriesExhausted extends TcpSenderState

private[sender] sealed trait TcpSenderData
private[sender] case object NoData extends TcpSenderData
private[sender] case class DisconnectedData(failures: Failures) extends TcpSenderData
private[sender] case class ConnectedData(connection: ActorRef, failures: Failures) extends TcpSenderData

