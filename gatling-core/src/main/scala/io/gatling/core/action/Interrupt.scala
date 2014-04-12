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

import scala.annotation.tailrec

import akka.actor.Actor.Receive
import akka.actor.ActorRef
import io.gatling.core.session._
import io.gatling.core.result.message.KO
import io.gatling.core.result.writer.DataWriterClient
import io.gatling.core.util.TimeHelper.nowMillis

object Interrupt {

  @tailrec
  def exitBlocks(blocks: List[Block], until: Block, actor: ActorRef, session: Session, groupsToClose: List[GroupBlock]): Interruption = blocks match {

    case Nil => Interruption(actor, session, groupsToClose)

    case head :: tail => head match {
      case `until` => Interruption(actor, session, groupsToClose)

      case group: GroupBlock =>
        exitBlocks(tail, until, actor, session.exitGroup, group :: groupsToClose)

      case tryMap: TryMaxBlock =>
        exitBlocks(tail, until, actor, session.exitTryMax, groupsToClose)

      case counter: CounterBlock =>
        exitBlocks(tail, until, actor, session.exitLoop, groupsToClose)
    }
  }

  object InterruptOnExitASAPLoop {

    @tailrec
    private def interruptOnExitASAPLoop(session: Session, leftToRightBlocks: List[Block]): Option[Interruption] = leftToRightBlocks match {
      case Nil => None

      case head :: tail => head match {

        case ExitASAPLoopBlock(_, condition, loopActor) if !LoopBlock.continue(condition, session) =>
          val interruption = exitBlocks(session.blockStack, head, loopActor, session, Nil)
          Some(interruption)

        case _ => interruptOnExitASAPLoop(session, tail)
      }
    }

    def unapply(message: Any): Option[Interruption] = message match {
      case session: Session => interruptOnExitASAPLoop(session, session.blockStack.reverse)
      case _                => None
    }
  }

  object InterruptOnTryMax {

    @tailrec
    def interruptOnTryMax(session: Session, stack: List[Block]): Option[Interruption] = stack match {
      case Nil => None

      case head :: tail => head match {

        case TryMaxBlock(_, tryMaxActor, status) if status == KO =>
          val interruption = Interrupt.exitBlocks(session.blockStack, head, tryMaxActor, session, Nil)
          Some(interruption)

        case _ => interruptOnTryMax(session, tail)
      }
    }

    def unapply(message: Any): Option[Interruption] = message match {
      case session: Session => interruptOnTryMax(session, session.blockStack)
      case _                => None
    }
  }
}

case class Interruption(nextActor: ActorRef, nextSession: Session, groupsToClose: List[GroupBlock])

object Interruptable extends DataWriterClient {

  private def doInterrupt(interruption: Interruption): Unit = {
    val now = nowMillis
    import interruption._
    groupsToClose.foreach(writeGroupData(nextSession, _, now))
    nextActor ! nextSession
  }

  val interrupt: Receive = {
    case Interrupt.InterruptOnExitASAPLoop(interruption) => doInterrupt(interruption)
    case Interrupt.InterruptOnTryMax(interruption)       => doInterrupt(interruption)
  }
}

/**
 * An Action that can be interrupted/bypassed when some conditions are met.
 * For example: actions within a loop.
 */
trait Interruptable extends Chainable {

  val interrupt = Interruptable.interrupt orElse super.receive

  abstract override def receive = interrupt
}
