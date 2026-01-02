/*
 * Copyright 2011-2026 GatlingCorp (https://gatling.io)
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

import io.gatling.commons.util.Clock
import io.gatling.commons.validation.Validation
import io.gatling.core.action.{ Action, ExitableAction, SessionHook }
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.{ Expression, Session }
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

final class SseProcessUnmatchedInboundMessagesBuilder(
    sseName: Expression[String],
    f: (List[SseInboundMessage], Session) => Validation[Session]
) extends ActionBuilder
    with SseAction
    with NameGen {

  override def build(ctx: ScenarioContext, next: Action): Action = {

    val expression: Expression[Session] = session =>
      for {
        fsmName <- sseName(session)
        fsm <- fetchFsm(fsmName, session)
        messages = fsm.collectUnmatchedInboundMessages()
        newSession <- f(messages, session)
      } yield newSession

    new SessionHook(expression, genName("sseProcessUnmatchedMessages"), ctx.coreComponents.statsEngine, next) with ExitableAction {
      override def clock: Clock = ctx.coreComponents.clock
    }
  }
}
