/*
 * Copyright 2011-2020 GatlingCorp (https://gatling.io)
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

package io.gatling.decoupled.action

import java.util.UUID

import io.gatling.commons.util.Clock
import io.gatling.commons.validation.Validation
import io.gatling.core.action.{ Action, RequestAction }
import io.gatling.core.session.{ Expression, Session, _ }
import io.gatling.core.stats.StatsEngine
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

class WaitDecoupledResponseAction(requestName: String, nextAction: Action, id: UUID, ctx: ScenarioContext) extends RequestAction with NameGen {

  override def name: String = genName(requestName)

  override def requestName: Expression[String] = name.expressionSuccess

//  override protected def execute(session: Session): Unit = ???
  override def sendRequest(requestName: String, session: Session): Validation[Unit] = {

    // TODO notify actor that a request is pending https://trayio.atlassian.net/browse/PSP-1658

    io.gatling.commons.validation.Success(())

  }

  override def statsEngine: StatsEngine = ctx.coreComponents.statsEngine

  override def clock: Clock = ctx.coreComponents.clock

  override def next: Action = nextAction

}
