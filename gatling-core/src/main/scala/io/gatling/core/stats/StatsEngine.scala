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

package io.gatling.core.stats

import java.net.InetSocketAddress

import io.gatling.commons.stats.Status
import io.gatling.core.session.GroupBlock
import io.gatling.core.stats.writer._

import akka.actor.ActorRef
import io.netty.channel.ChannelDuplexHandler

trait StatsEngine extends FrontLineStatsEngineExtensions {

  def start(): Unit

  def stop(controller: ActorRef, exception: Option[Exception]): Unit

  def logUserStart(scenario: String, timestamp: Long): Unit

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
  //
  //
  // [fl]

  def logResponse(
      scenario: String,
      groups: List[String],
      requestName: String,
      startTimestamp: Long,
      endTimestamp: Long,
      status: Status,
      responseCode: Option[String],
      message: Option[String]
  ): Unit

  def logGroupEnd(
      scenario: String,
      groupBlock: GroupBlock,
      exitTimestamp: Long
  ): Unit

  def logCrash(scenario: String, groups: List[String], requestName: String, error: String): Unit

  def reportUnbuildableRequest(scenario: String, groups: List[String], requestName: String, errorMessage: String): Unit =
    logCrash(scenario, groups, requestName, s"Failed to build request: $errorMessage")
}

// WARNING those methods only serve a purpose in FrontLine and mustn't be called from other components
trait FrontLineStatsEngineExtensions {
  final def statsChannelHandler: ChannelDuplexHandler = null

  final def logTcpConnectAttempt(remoteAddress: InetSocketAddress): Unit = {}

  final def logTcpConnect(remoteAddress: String, startTimestamp: Long, endTimestamp: Long, error: Option[String]): Unit = {}

  final def logTlsHandshake(remoteAddress: String, startTimestamp: Long, endTimestamp: Long, error: Option[String]): Unit = {}
}
