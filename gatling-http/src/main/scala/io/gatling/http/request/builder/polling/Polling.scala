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
package io.gatling.http.request.builder.polling

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.session._
import io.gatling.http.action.async.polling.{ PollingStartBuilder, PollingStopBuilder }
import io.gatling.http.request.builder.HttpRequestBuilder

object Polling {
  val DefaultPollerName = SessionPrivateAttributes.PrivateAttributePrefix + "http.polling"
}
class Polling(pollerName: String = Polling.DefaultPollerName) {

  def pollerName(pollerName: String) = new Polling(pollerName)

  def every(period: Expression[FiniteDuration]) = new PollingEveryStep(pollerName, period)

  def stop = new PollingStopBuilder(pollerName)
}

class PollingEveryStep(pollerName: String, period: Expression[FiniteDuration]) {

  def exec(requestBuilder: HttpRequestBuilder) =
    new PollingStartBuilder(pollerName, period, requestBuilder)
}
