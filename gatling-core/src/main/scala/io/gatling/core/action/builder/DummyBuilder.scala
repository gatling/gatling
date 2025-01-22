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

package io.gatling.core.action.builder

import io.gatling.commons.validation.SuccessWrapper
import io.gatling.core.action.{ Action, Dummy }
import io.gatling.core.session.{ Expression, Session, TrueExpressionSuccess }
import io.gatling.core.structure.ScenarioContext

import com.typesafe.scalalogging.StrictLogging

private[gatling] object DummyBuilder {
  def apply(requestName: Expression[String], responseTimeInMillis: Expression[Int]): DummyBuilder =
    new DummyBuilder(requestName, responseTimeInMillis, success = TrueExpressionSuccess, sessionUpdate = session => session.success)

}

private[gatling] final class DummyBuilder(
    requestName: Expression[String],
    responseTimeInMillis: Expression[Int],
    success: Expression[Boolean],
    sessionUpdate: Expression[Session]
) extends ActionBuilder
    with StrictLogging {

  def withSuccess(newSuccess: Expression[Boolean]): DummyBuilder =
    new DummyBuilder(requestName, responseTimeInMillis, newSuccess, sessionUpdate)

  def withSessionUpdate(newSessionUpdate: Expression[Session]): DummyBuilder =
    new DummyBuilder(requestName, responseTimeInMillis, success, newSessionUpdate)

  override def build(ctx: ScenarioContext, next: Action): Action =
    new Dummy(requestName, responseTimeInMillis, success, sessionUpdate, ctx.coreComponents.statsEngine, ctx.coreComponents.clock, next)
}
