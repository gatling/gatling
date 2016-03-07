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
package io.gatling.core.action

import scala.collection.mutable

import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

import akka.actor.{ ActorSystem, Props }

object RendezVous extends NameGen {

  def apply(users: Int, system: ActorSystem, statsEngine: StatsEngine, next: Action): Action = {
    val actor = system.actorOf(RendezVousActor.props(users, next))
    new ExitableActorDelegatingAction(genName("rendezVous"), statsEngine, next, actor)
  }
}

object RendezVousActor {
  def props(users: Int, next: Action) =
    Props(new RendezVousActor(users: Int, next))
}

/**
 * Buffer Sessions until users is reached, then unleash buffer and become passthrough.
 */
class RendezVousActor(users: Int, val next: Action) extends ActionActor {

  val buffer = mutable.Queue.empty[Session]

  val passThrough: Receive = {
    case session: Session => next ! session
  }

  def execute(session: Session): Unit = {
    buffer += session
    if (buffer.length == users) {
      context.become(passThrough)
      buffer.foreach(next ! _)
      buffer.clear()
    }
  }
}
