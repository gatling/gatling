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

package io.gatling.core.action

import scala.collection.mutable

import io.gatling.commons.util.Clock
import io.gatling.core.akka.BaseActor
import io.gatling.core.session.Session
import io.gatling.core.stats.StatsEngine
import io.gatling.core.util.NameGen

import akka.actor.{ ActorRef, ActorSystem, Props }

object RendezVous extends NameGen {
  def apply(users: Int, actorSystem: ActorSystem, statsEngine: StatsEngine, clock: Clock, next: Action): RendezVous = {
    val props = Props(new RendezVousActor(users: Int, next))
    val actor = actorSystem.actorOf(props, genName("rendezVous"))
    new RendezVous(actor, statsEngine, clock, next)
  }
}

class RendezVous private (actor: ActorRef, val statsEngine: StatsEngine, val clock: Clock, val next: Action)
    extends ActorDelegatingAction(actor.path.name, actor)

/**
 * Buffer Sessions until users is reached, then unleash buffer and become passthrough.
 */
class RendezVousActor(users: Int, val next: Action) extends BaseActor {

  private val buffer = mutable.Queue.empty[Session]

  private val passThrough: Receive = { case session: Session =>
    next ! session
  }

  def execute(session: Session): Unit = {
    buffer += session
    if (buffer.length == users) {
      context.become(passThrough)
      buffer.foreach(next ! _)
      buffer.clear()
    }
  }

  override def receive: Receive = { case session: Session =>
    execute(session)
  }

  /**
   * Makes sure that in case of an actor crash, the Session is not lost but passed to the next Action.
   */
  override def preRestart(reason: Throwable, message: Option[Any]): Unit =
    message.foreach {
      case session: Session =>
        logger.error(s"'${self.path.name}' crashed on session $session, forwarding to the next one", reason)
        next ! session.markAsFailed
      case _ =>
        logger.error(s"'${self.path.name}' crashed on unknown message $message, dropping", reason)
    }
}
