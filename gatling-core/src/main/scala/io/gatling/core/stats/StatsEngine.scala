/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.core.stats

import java.net.InetSocketAddress

import io.gatling.commons.stats.Status
import io.gatling.core.session.{ GroupBlock, Session }
import io.gatling.core.stats.writer._

import akka.actor.ActorRef
import io.netty.channel.ChannelDuplexHandler

trait StatsEngine extends FrontLineStatsEngineExtensions {

  def start(): Unit

  def stop(controller: ActorRef, exception: Option[Exception]): Unit

  def logUserStart(session: Session): Unit

  def logUserEnd(userMessage: UserEndMessage): Unit

  // [fl]
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  //
  // [fl]

  def logResponse(
      session: Session,
      requestName: String,
      startTimestamp: Long,
      endTimestamp: Long,
      status: Status,
      responseCode: Option[String],
      message: Option[String]
  ): Unit

  def logGroupEnd(
      session: Session,
      group: GroupBlock,
      exitTimestamp: Long
  ): Unit

  def logCrash(session: Session, requestName: String, error: String): Unit

  def reportUnbuildableRequest(session: Session, requestName: String, errorMessage: String): Unit =
    logCrash(session, requestName, s"Failed to build request: $errorMessage")

  def statsChannelHandler: ChannelDuplexHandler = null
}

// WARNING those methods only serve a purpose in FrontLine and mustn't be called from other components
trait FrontLineStatsEngineExtensions {
  final def logTcpConnectAttempt(remoteAddress: InetSocketAddress): Unit = {}

  final def logTcpConnect(remoteAddress: String, startTimestamp: Long, endTimestamp: Long, error: Option[String]): Unit = {}

  final def logTlsHandshake(remoteAddress: String, startTimestamp: Long, endTimestamp: Long, error: Option[String]): Unit = {}
}
