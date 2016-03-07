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
package io.gatling.http.request.builder.sse

import io.gatling.core.session.{ SessionPrivateAttributes, Expression }
import io.gatling.http.action.async.sse._
import io.gatling.http.check.async.AsyncCheckBuilder

object Sse {
  val DefaultSseName = SessionPrivateAttributes.PrivateAttributePrefix + "http.sse"
}

class Sse(requestName: Expression[String], sseName: String = Sse.DefaultSseName) {

  def sseName(sseName: String) = new Sse(requestName, sseName)
  def open(url: Expression[String]) = SseOpenRequestBuilder(requestName, url, sseName)
  def check(checkBuilder: AsyncCheckBuilder) = new SseSetCheckBuilder(requestName, checkBuilder, sseName)
  def cancelCheck = new SseCancelCheckBuilder(requestName, sseName)
  def reconciliate() = new SseReconciliateBuilder(requestName, sseName)
  def close() = new SseCloseBuilder(requestName, sseName)
}
