/*
 * Copyright 2011-2024 GatlingCorp (https://gatling.io)
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

package io.gatling.core.action

import io.gatling.core.controller.inject.InjectorCommand
import io.gatling.core.session.Session

import org.apache.pekko.actor.ActorRef

private[gatling] final class Exit(injector: ActorRef) extends Action {
  override val name = "gatling-exit"

  def execute(session: Session): Unit = {
    logger.debug(s"End user #${session.userId}")
    session.exit()
    injector ! InjectorCommand.UserEnd(session.scenario)
  }
}
