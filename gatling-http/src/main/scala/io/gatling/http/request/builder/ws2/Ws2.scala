/*
 * Copyright 2011-2017 GatlingCorp (http://gatling.io)
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

package io.gatling.http.request.builder.ws2

import io.gatling.core.session._
import io.gatling.http.action.ws2._
import io.gatling.http.request.builder.CommonAttributes

object Ws2 {

  private val DefaultWebSocketName = SessionPrivateAttributes.PrivateAttributePrefix + "http.webSocket"

  def apply(requestName: Expression[String], wsName: String = DefaultWebSocketName): Ws2 = new Ws2(requestName, wsName)

  def checkTextMessage(name: String) = WsCheck(name, Nil, Nil)
}

/**
 * @param requestName The name of this request
 * @param wsName The name of the session attribute used to store the websocket
 */
class Ws2(requestName: Expression[String], wsName: String) {

  def wsName(wsName: String) = new Ws2(requestName, wsName)

  /**
   * Opens a web socket and stores it in the session.
   *
   * @param url The socket URL
   *
   */
  def connect(url: Expression[String]) = WsConnectRequestBuilder(CommonAttributes(requestName, "GET", Left(url)), wsName)

  /**
   * Sends a text message on the given websocket.
   *
   * @param text The message
   */
  def sendText(text: Expression[String]) = WsSendBuilder(requestName, wsName, text, Nil)

  /**
   * Closes a websocket.
   */
  def close = new WsCloseBuilder(requestName, wsName)
}
