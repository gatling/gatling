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

package io.gatling.core.action

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

import io.gatling.core.actor.{ Actor, ActorRef, ActorSystem, Behavior, Cancellable }
import io.gatling.core.session.Session
import io.gatling.core.util.NameGen

private object RendezVous extends NameGen {
  def apply(users: Int, timeout: Option[FiniteDuration], actorSystem: ActorSystem, next: Action): RendezVous = {
    val name = genName("rendezVous")
    val actor = actorSystem.actorOf(new RendezVousActor(users, timeout, next, name))
    new RendezVous(name, actor)
  }
}

private final class RendezVous private (val name: String, actor: ActorRef[RendezVousActor.Command]) extends Action {
  override def execute(session: Session): Unit = actor ! RendezVousActor.Command.Arrive(session)
}

private object RendezVousActor {
  sealed trait Command

  object Command {
    final case class Arrive(session: Session) extends Command
    final case class Timeout(batch: Long) extends Command
  }
}

/**
 * Buffer Sessions until users is reached, then unleash buffer and re-arm for the next batch.
 *
 * When a timeout is defined, it starts when the first Session of a batch arrives. If the batch isn't complete when it fires, the Sessions buffered so far are
 * unleashed anyway.
 */
private final class RendezVousActor(users: Int, timeout: Option[FiniteDuration], next: Action, name: String) extends Actor[RendezVousActor.Command](name) {
  import RendezVousActor.Command._

  private val buffer = mutable.Queue.empty[Session]
  // identifies the current batch so a timeout that fires after its batch has been released is ignored
  private var batch = 0L
  private var timer: Option[Cancellable] = None

  private def release(): Unit = {
    buffer.foreach(next ! _)
    buffer.clear()
    timer.foreach(_.cancel())
    timer = None
    batch += 1
  }

  override def init(): Behavior[RendezVousActor.Command] = {
    case Arrive(session) =>
      buffer += session
      if (buffer.sizeIs == users) {
        release()
      } else if (buffer.sizeIs == 1) {
        timer = timeout.map { t =>
          val currentBatch = batch
          scheduler.scheduleOnce(t)(self ! Timeout(currentBatch))
        }
      }
      stay

    case Timeout(timedOutBatch) =>
      if (timedOutBatch == batch) {
        logger.debug(s"Rendez-vous $name timed out with ${buffer.size}/$users users, releasing them")
        release()
      }
      stay
  }
}
