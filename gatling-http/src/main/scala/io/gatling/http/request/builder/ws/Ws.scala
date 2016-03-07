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
package io.gatling.http.request.builder.ws

import io.gatling.core.session.{ Expression, SessionPrivateAttributes }
import io.gatling.http.action.async.ws._
import io.gatling.http.check.async.AsyncCheckBuilder
import io.gatling.http.request.builder.CommonAttributes

object Ws {

  val DefaultWebSocketName = SessionPrivateAttributes.PrivateAttributePrefix + "http.webSocket"
}

/**
 * @param requestName The name of this request
 * @param wsName The name of the session attribute used to store the websocket
 */
class Ws(requestName: Expression[String], wsName: String = Ws.DefaultWebSocketName) {

  def wsName(wsName: String) = new Ws(requestName, wsName)

  /**
   * Opens a web socket and stores it in the session.
   *
   * @param url The socket URL
   *
   */
  def open(url: Expression[String]) = new WsOpenRequestBuilder(CommonAttributes(requestName, "GET", Left(url)), wsName)

  /**
   * Sends a binary message on the given websocket.
   *
   * @param bytes The message
   */
  def sendBytes(bytes: Expression[Array[Byte]]) = new WsSendBuilder(requestName, wsName, bytes.map(BinaryMessage))

  /**
   * Sends a text message on the given websocket.
   *
   * @param text The message
   */
  def sendText(text: Expression[String]) = new WsSendBuilder(requestName, wsName, text.map(TextMessage))

  /**
   * Check for incoming messages on the given websocket.
   *
   * @param checkBuilder The check builder
   */
  def check(checkBuilder: AsyncCheckBuilder) = new WsSetCheckBuilder(requestName, checkBuilder, wsName)

  /**
   * Cancel current check on the given websocket.
   *
   */
  def cancelCheck = new WsCancelCheckBuilder(requestName, wsName)

  /**
   * Reconciliate the main state with the one of the websocket flow.
   */
  def reconciliate = new WsReconciliateBuilder(requestName, wsName)

  /**
   * Closes a websocket.
   */
  def close = new WsCloseBuilder(requestName, wsName)
}
