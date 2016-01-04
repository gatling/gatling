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
package io.gatling.core.stats.writer

import akka.actor.FSM
import io.gatling.core.akka.BaseActor
import io.gatling.core.config.GatlingConfiguration

private[writer] trait DataWriterFSM extends BaseActor with FSM[DataWriterState, DataWriterData]

private[writer] sealed trait DataWriterState
private[writer] case object Uninitialized extends DataWriterState
private[writer] case object Initialized extends DataWriterState
private[writer] case object Terminated extends DataWriterState

trait DataWriterData
private[writer] case object NoData extends DataWriterData
case class InitData(configuration: GatlingConfiguration) extends DataWriterData
