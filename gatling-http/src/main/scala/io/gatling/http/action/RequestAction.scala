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
package io.gatling.http.action

import io.gatling.commons.validation.Validation
import io.gatling.core.action.ExitableAction
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine

abstract class RequestAction(val statsEngine: StatsEngine) extends ExitableAction {

  def requestName: Expression[String]
  def sendRequest(requestName: String, session: Session): Validation[Unit]

  override def execute(session: Session): Unit = recover(session) {
    requestName(session).flatMap { resolvedRequestName =>
      val outcome = sendRequest(resolvedRequestName, session)
      outcome.onFailure(errorMessage => statsEngine.reportUnbuildableRequest(session, resolvedRequestName, errorMessage))
      outcome
    }
  }
}
