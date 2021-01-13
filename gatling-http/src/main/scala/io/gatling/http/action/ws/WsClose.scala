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

package io.gatling.http.action.ws

import io.gatling.commons.util.Clock
import io.gatling.commons.validation.Validation
import io.gatling.core.action.{ Action, ExitableAction, RequestAction }
import io.gatling.core.session.{ Session, _ }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

class WsClose(
    override val requestName: Expression[String],
    wsName: Expression[String],
    override val statsEngine: StatsEngine,
    override val clock: Clock,
    val next: Action
) extends RequestAction
    with WsAction
    with ExitableAction
    with NameGen {

  override val name: String = genName("wsClose")

  override def sendRequest(requestName: String, session: Session): Validation[Unit] =
    for {
      fsmName <- wsName(session)
      fsm <- fetchFsm(fsmName, session)
    } yield {
      logger.debug(s"Closing websocket '$wsName': Scenario '${session.scenario}', UserId #${session.userId}")
      fsm.onClientCloseRequest(requestName, session, next)
    }
}
