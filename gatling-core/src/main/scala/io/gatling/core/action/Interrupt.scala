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

  /**
   * Recursively loop on a block List until a stop point and build the Interruption to execute.
   *
   * @param blocks the blocks to scan
   * @param until the exit point of the loop
   * @param actor the next actor to chain when resolving the Interruption
   * @param session the session so far
   * @param groupsToClose the groups so far to close when resolving the Interruption
   * @return the Interruption to process
   */
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

  /**
   * An extractor to determine if this session is supposed to trigger an Interruption based on ExitASAP loops
   * (a loop that doesn't wait for iteration end before testing exit condition).
   */
  object InterruptOnExitASAPLoop {

    /**
     * Scan the block stack for ExitASAP loops.
     * Scan is performed from left to right (right is deeper) = reversed.
     *
     * @param session the session
     * @param leftToRightBlocks the blocks sorted from left to right
     * @return the potential Interruption to process
     */
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

  /**
   * An extractor to determine if this session is supposed to trigger an Interruption based on TryMax blocks.
   */
  object InterruptOnTryMax {

    /**
     * Scan the block stack for ExitASAP loops.
     * Scan is performed from right to left (right is deeper) = normal.
     *
     * @param session the session
     * @param stack the blocks sorted from right to left
     * @return the potential Interruption to process
     */
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

/**
 * Describes an interruption to be performed.
 * @param nextActor the actor to send next, instead of following the regular workflow.
 * @param nextSession the new Session to be sent to nextActor
 * @param groupsToClose the groups to be closed as we bypass the regular GroupEnd from the regular flow
 */
case class Interruption(nextActor: ActorRef, nextSession: Session, groupsToClose: List[GroupBlock])

object Interruptable extends DataWriterClient {

  private def doInterrupt(interruption: Interruption): Unit = {
    val now = nowMillis
    import interruption._
    groupsToClose.foreach(writeGroupData(nextSession, _, now))
    nextActor ! nextSession
  }

  /**
   * Check for ExitASAP loops and TryMax blocks that might interrupt the regular flow.
   * This logic is not directly in Interruptable trait as Interruptable behavior can me mixed in dynamically.
   * For example, loops and trymax blocks become interruptable once they've become initialized with the loop content.
   */
  val interrupt: Receive = {
    case Interrupt.InterruptOnExitASAPLoop(interruption) => doInterrupt(interruption)
    case Interrupt.InterruptOnTryMax(interruption)       => doInterrupt(interruption)
  }
}

/**
 * An Action that can trigger an interrupt and bypass regular workflow.
 */
trait Interruptable extends Chainable {

  val interrupt = Interruptable.interrupt orElse super.receive

  abstract override def receive = interrupt
}
