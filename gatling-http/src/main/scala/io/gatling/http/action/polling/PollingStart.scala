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

package io.gatling.http.action.polling

import scala.concurrent.duration.FiniteDuration

import io.gatling.commons.util.Clock
import io.gatling.commons.validation.{ Failure, Validation }
import io.gatling.core.CoreComponents
import io.gatling.core.action.{ Action, ExitableAction }
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen
import io.gatling.http.cache.HttpCaches
import io.gatling.http.engine.tx.HttpTxExecutor
import io.gatling.http.protocol.HttpProtocol
import io.gatling.http.request.HttpRequestDef

class PollingStart(
    pollerName: String,
    period: FiniteDuration,
    coreComponents: CoreComponents,
    httpRequestDef: HttpRequestDef,
    httpCaches: HttpCaches,
    httpProtocol: HttpProtocol,
    httpTxExecutor: HttpTxExecutor,
    val next: Action
) extends ExitableAction
    with NameGen {

  override val name: String = genName(pollerName)

  override def clock: Clock = coreComponents.clock

  override def statsEngine: StatsEngine = coreComponents.statsEngine

  private def startPoller(session: Session): Session = {
    logger.debug(s"Starting poller $pollerName")
    val poller = new Poller(
      pollerName,
      period,
      httpRequestDef,
      httpTxExecutor,
      httpCaches,
      httpProtocol,
      statsEngine
    )

    val newSession = session.set(pollerName, poller)
    poller.start(newSession)
    newSession
  }

  override def execute(session: Session): Unit =
    recover(session) {
      if (session.contains(pollerName)) {
        Failure(s"Unable to create a new poller with name $pollerName: already exists")
      } else {
        val newSession = startPoller(session)
        next ! newSession
        Validation.unit
      }
    }
}
