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
package io.gatling.http.action.ws2

import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.session.{ Session, SessionPrivateAttributes }
import io.gatling.core.structure.ScenarioContext
import io.gatling.core.util.NameGen

object OnConnectedChainEndActionBuilder extends ActionBuilder with NameGen {

  override def build(ctx: ScenarioContext, next: Action): Action =
    new OnConnectedChainEndAction(genName("connectChainEnd"), ctx.coreComponents.exit)
}

object OnConnectedChainEndAction {

  val NextAction = SessionPrivateAttributes.PrivateAttributePrefix + "nextAction"
}

/**
 * Acts as the last action of a connect sequence.
 * Next action is fetched from the session
 *
 * @param name action name
 * @param exit exit action if next was not found in the session
 */
class OnConnectedChainEndAction(override val name: String, exit: Action) extends Action {

  override def execute(session: Session): Unit =
    session(OnConnectedChainEndAction.NextAction).validate[Action]
      .map {
        _ ! session.remove(OnConnectedChainEndAction.NextAction)
      }.onFailure { message =>
        logger.error(s"'$name' failed to execute: $message")
        exit ! session
      }
}
