/*
 * Copyright 2011-2019 GatlingCorp (https://gatling.io)
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

package io.gatling.http.action.sse

import io.gatling.core.session.{ Expression, SessionPrivateAttributes }
import io.gatling.http.check.sse.SseMessageCheck
import io.gatling.http.request.builder.sse.SseConnectRequestBuilder

object Sse {
  private val DefaultSseName = SessionPrivateAttributes.PrivateAttributePrefix + "http.sse"

  def apply(requestName: Expression[String], sseName: String = DefaultSseName): Sse = new Sse(requestName, sseName)

  def checkMessage(name: String) = SseMessageCheck(name, Nil, Nil)
}

class Sse(requestName: Expression[String], sseName: String) {

  def sseName(sseName: String) = new Sse(requestName, sseName)

  def connect(url: Expression[String]) = SseConnectRequestBuilder(requestName, url, sseName)

  def setCheck = SseSetCheckBuilder(requestName, sseName, Nil)

  def close() = SseCloseBuilder(requestName, sseName)
}
