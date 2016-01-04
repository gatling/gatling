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
package io.gatling.http.action.async

import io.gatling.core.session.Session

import akka.actor.ActorRef

trait AsyncProtocolAction {

  // import optimized TypeCaster
  import io.gatling.http.util.HttpTypeHelper._

  def actorFetchErrorMessage: String

  final def fetchActor(actorName: String, session: Session) =
    session(actorName)
      .validate[ActorRef]
      .mapError(m => s"$actorFetchErrorMessage: $m")
}
