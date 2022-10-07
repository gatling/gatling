/*
 * Copyright 2011-2022 GatlingCorp (https://gatling.io)
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

import scala.concurrent.duration.FiniteDuration

import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session._
import io.gatling.http.check.ws.WsFrameCheck

trait WsAwaitActionBuilder[T] extends ActionBuilder {
  // we need this override because we can't add an Int => Expression[FiniteDuration] that would clash with Int => Expression[Any]
  def await(timeout: FiniteDuration)(checks: WsFrameCheck*): T =
    await(timeout.expressionSuccess)(checks: _*)

  def await(timeout: Expression[FiniteDuration])(checks: WsFrameCheck*): T = {
    require(!checks.contains(null), "Checks can't contain null elements. Forward reference issue?")
    appendCheckSequence(WsFrameCheckSequenceBuilder(timeout, checks.toList))
  }

  protected def appendCheckSequence(checkSequence: WsFrameCheckSequenceBuilder[WsFrameCheck]): T
}
