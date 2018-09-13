/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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

import io.gatling.core.session._
import io.gatling.http.check.ws.{ WsBinaryFrameCheck, WsTextFrameCheck }
import io.gatling.http.request.builder.CommonAttributes
import io.gatling.http.request.builder.ws.WsConnectRequestBuilder

import io.netty.handler.codec.http.HttpMethod

object Ws {

  private val DefaultWebSocketName = SessionPrivateAttributes.PrivateAttributePrefix + "http.webSocket"

  def apply(requestName: Expression[String], wsName: String = DefaultWebSocketName): Ws = new Ws(requestName, wsName)

  def checkTextMessage(name: String) = WsTextFrameCheck(name, Nil, Nil)

  def checkBinaryMessage(name: String) = WsBinaryFrameCheck(name, Nil, Nil)
}

/**
 * @param requestName The name of this request
 * @param wsName The name of the session attribute used to store the WebSocket
 */
class Ws(requestName: Expression[String], wsName: String) {

  def wsName(wsName: String) = new Ws(requestName, wsName)

  /**
   * Opens a WebSocket and stores it in the session.
   *
   * @param url The socket URL
   *
   */
  def connect(url: Expression[String]) = WsConnectRequestBuilder(CommonAttributes(requestName, HttpMethod.GET, Left(url)), wsName)

  /**
   * Sends a text frame on the given WebSocket.
   *
   * @param text The message
   */
  def sendText(text: Expression[String]) = WsSendTextFrameBuilder(requestName, wsName, text, Nil)

  /**
   * Sends a binary frame on the given WebSocket.
   *
   * @param bytes The message
   */
  def sendBytes(bytes: Expression[Array[Byte]]) = WsSendBinaryFrameBuilder(requestName, wsName, bytes, Nil)

  /**
   * Closes a WebSocket.
   */
  def close = new WsCloseBuilder(requestName, wsName)
}
