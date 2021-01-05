/*
 * Copyright 2011-2021 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.Clock
import io.gatling.commons.validation._
import io.gatling.core.CoreComponents
import io.gatling.core.action.{ Action, RequestAction }
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.http.engine.tx.{ HttpTx, HttpTxExecutor }
import io.gatling.http.request.HttpRequestDef

/**
 * This is an action that sends HTTP requests
 */
class HttpRequestAction(
    httpRequestDef: HttpRequestDef,
    httpTxExecutor: HttpTxExecutor,
    coreComponents: CoreComponents,
    val next: Action
) extends RequestAction
    with NameGen {

  override def clock: Clock = coreComponents.clock

  override val name: String = genName("httpRequest")

  override def requestName: Expression[String] = httpRequestDef.requestName

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  override def sendRequest(requestName: String, session: Session): Validation[Unit] =
    httpRequestDef.build(requestName, session).map { httpRequest =>
      val tx = HttpTx(
        session,
        httpRequest,
        next,
        None,
        0
      )

      httpTxExecutor.execute(tx)
    }
}
