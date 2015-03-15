/**
 * Copyright 2011-2014 eBusiness Information, Groupe Excilys (www.ebusinessinformation.fr)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gatling.core.action

import akka.actor.ActorDSL.actor
import akka.actor.ActorRef
import io.gatling.core.akka.AkkaDefaults
import io.gatling.core.controller.Controller
import io.gatling.core.result.message.End
import io.gatling.core.result.writer.UserMessage
import io.gatling.core.session.Session
import io.gatling.core.util.TimeHelper.nowMillis

object UserEnd extends AkkaDefaults {

  def userEnd(): ActorRef = actor("userEnd")(new UserEnd)
}

class UserEnd extends Action {

  def execute(session: Session): Unit = {
    session.terminate()
    Controller ! UserMessage(session, End, nowMillis)
  }
}
