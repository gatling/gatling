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

package io.gatling.http.action.sse

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.action.Action
import io.gatling.core.session.Expression
import io.gatling.core.structure.ScenarioContext
import io.gatling.http.action.HttpActionBuilder
import io.gatling.http.check.sse.{ SseMessageCheck, SseMessageCheckSequence }
import io.gatling.http.request.builder.sse.SseConnectRequestBuilder

import com.softwaremill.quicklens._

case class SseConnectBuilder(
    requestName:    Expression[String],
    sseName:        String,
    requestBuilder: SseConnectRequestBuilder,
    checkSequences: List[SseMessageCheckSequence]
) extends HttpActionBuilder {

  def await(timeout: FiniteDuration)(checks: SseMessageCheck*): SseConnectBuilder =
    this.modify(_.checkSequences).using(_ ::: List(SseMessageCheckSequence(timeout, checks.toList)))

  override def build(ctx: ScenarioContext, next: Action): Action = {
    import ctx._
    val httpComponents = lookUpHttpComponents(protocolComponentsRegistry)
    val request = requestBuilder.build(httpComponents)
    new SseConnect(requestName, sseName, request, checkSequences, httpComponents, next)
  }
}
