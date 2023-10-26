/*
 * Copyright 2011-2023 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.ws.fsm

import io.gatling.commons.stats.{ KO, Status }
import io.gatling.commons.util.StringHelper.Eol
import io.gatling.core.session.Session
import io.gatling.http.engine.response.HttpTracing
import io.gatling.http.util._
import io.gatling.shared.util.StringBuilderPool

import com.typesafe.scalalogging.Logger

object WsLogger {
  private val loggingStringBuilderPool = new StringBuilderPool
  private val logger = Logger(HttpTracing.LOGGER)

  private def logCheck(
      requestName: String,
      session: Session,
      status: Status,
      inboundMessages: Seq[(Long, String)],
      errorMessage: Option[String],
      checkName: Option[String],
      message: Option[String]
  ): Unit = {
    def dump = loggingStringBuilderPool
      .get()
      .append(Eol)
      .appendWithEol(">>>>>>>>>>>>>>>>>>>>>>>>>>")
      .appendWithEol("Request:")
      .appendWithEol(s"$requestName: $status ${errorMessage.getOrElse("")}")
      .appendWithEol("=========================")
      .appendWithEol("Session:")
      .append(session)
      .append(Eol)
      .appendWithEol("=========================")
      .appendWithEol(s"WebSocket check:")
      .appendWithEol(checkName.getOrElse(""))
      .appendWithEol("=========================")
      .appendWithEol("WebSocket request:")
      .appendWithEol(message.getOrElse(""))
      .appendWithEol("=========================")
      .appendWithEol("WebSocket received messages:")
      .appendWebsocketInboundMessages(inboundMessages)
      .append("<<<<<<<<<<<<<<<<<<<<<<<<<")
      .toString

    if (status == KO) {
      logger.debug(s"Request '$requestName' failed for user ${session.userId}: ${errorMessage.getOrElse("")}")
      if (!HttpTracing.IS_HTTP_TRACE_ENABLED) {
        logger.debug(dump)
      }
    }

    logger.trace(dump)
  }

  private def logOk(
      requestName: String,
      session: Session,
      inboundMessages: Seq[(Long, String)],
      message: Option[String]
  ): Unit = {
    def dump = loggingStringBuilderPool
      .get()
      .append(Eol)
      .appendWithEol(">>>>>>>>>>>>>>>>>>>>>>>>>>")
      .appendWithEol("Request:")
      .appendWithEol(s"$requestName: OK")
      .appendWithEol("=========================")
      .appendWithEol("Session:")
      .append(session)
      .append(Eol)
      .appendWithEol("=========================")
      .appendWithEol("WebSocket request:")
      .appendWithEol(message.getOrElse(""))
      .appendWithEol("=========================")
      .appendWithEol("WebSocket received messages:")
      .appendWebsocketInboundMessages(inboundMessages)
      .append("<<<<<<<<<<<<<<<<<<<<<<<<<")
      .toString

    logger.trace(dump)
  }
}

final class WsLogger {

  private var inboundMessages: List[(Long, String)] = Nil

  def registerInboundMessage(message: String, timestamp: Long): Unit =
    if (HttpTracing.IS_HTTP_DEBUG_ENABLED) {
      inboundMessages = (timestamp, message) :: inboundMessages
    }

  def registerInboundMessage(message: Array[Byte], timestamp: Long): Unit =
    if (HttpTracing.IS_HTTP_DEBUG_ENABLED) {
      inboundMessages = (timestamp, s"<<<BINARY CONTENT length=${message.length}>>>") :: inboundMessages
    }

  def logCheck(
      requestName: String,
      session: Session,
      status: Status,
      errorMessage: Option[String],
      checkName: Option[String],
      message: Option[String]
  ): Unit = {
    WsLogger.logCheck(
      requestName,
      session,
      status: Status,
      inboundMessages.reverse,
      errorMessage,
      checkName,
      message
    )
    inboundMessages = Nil
  }

  def logOk(
      requestName: String,
      session: Session,
      message: Option[String]
  ): Unit = {
    WsLogger.logOk(
      requestName,
      session,
      inboundMessages.reverse,
      message
    )
    inboundMessages = Nil
  }
}
