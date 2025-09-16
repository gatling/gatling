/*
 * Copyright 2011-2025 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.ws

import io.gatling.commons.validation.Validation
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session._
import io.gatling.http.check.ws.WsFrameCheck
import io.gatling.http.request.builder.CommonAttributes
import io.gatling.http.request.builder.ws.WsConnectRequestBuilder

import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus

object Ws {
  private val DefaultWebSocketName = SessionPrivateAttributes.generatePrivateAttribute("http.webSocket").expressionSuccess

  def apply(requestName: Expression[String]): Ws = apply(requestName, DefaultWebSocketName)

  def apply(requestName: Expression[String], wsName: Expression[String]): Ws = new Ws(requestName, wsName)

  def checkTextMessage(name: Expression[String]): WsFrameCheck.Text = WsFrameCheck.Text(name, Nil, Nil, isSilent = false, resolvedName = "")

  def checkBinaryMessage(name: Expression[String]): WsFrameCheck.Binary = WsFrameCheck.Binary(name, Nil, Nil, isSilent = false, resolvedName = "")

  def processUnmatchedMessages(f: (List[WsInboundMessage], Session) => Validation[Session]): ActionBuilder =
    new WsProcessUnmatchedInboundMessagesBuilder(DefaultWebSocketName, f)

  def processUnmatchedMessages(wsName: Expression[String], f: (List[WsInboundMessage], Session) => Validation[Session]): ActionBuilder =
    new WsProcessUnmatchedInboundMessagesBuilder(wsName, f)
}

/**
 * @param requestName
 *   The name of this request
 * @param wsName
 *   The name of the session attribute used to store the WebSocket
 */
final class Ws(requestName: Expression[String], wsName: Expression[String]) {
  def wsName(wsName: Expression[String]): Ws = new Ws(requestName, wsName)

  /**
   * Opens a WebSocket and stores it in the session.
   *
   * @param url
   *   The socket URL
   */
  def connect(url: Expression[String]): WsConnectRequestBuilder =
    WsConnectRequestBuilder(CommonAttributes(requestName, Right(HttpMethod.GET), Left(url)), wsName, None, None, Nil)

  /**
   * Sends a text frame on the given WebSocket.
   *
   * @param text
   *   The message
   */
  def sendText(text: Expression[String]): WsSendTextFrameBuilder = WsSendTextFrameBuilder(requestName, wsName, text, Nil)

  /**
   * Sends a binary frame on the given WebSocket.
   *
   * @param bytes
   *   The message
   */
  def sendBytes(bytes: Expression[Array[Byte]]): WsSendBinaryFrameBuilder = WsSendBinaryFrameBuilder(requestName, wsName, bytes, Nil)

  /**
   * Closes a WebSocket with a 1000 status.
   */
  def close: WsCloseBuilder = new WsCloseBuilder(requestName, wsName, WebSocketCloseStatus.NORMAL_CLOSURE)

  /**
   * Closes a WebSocket with specified status code and reason.
   */
  def close(statusCode: Int, reason: String): WsCloseBuilder = new WsCloseBuilder(requestName, wsName, new WebSocketCloseStatus(statusCode, reason))
}
