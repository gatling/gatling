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

import scala.annotation.tailrec

import io.gatling.commons.stats.KO
import io.gatling.core.session._
import io.gatling.core.stats.StatsEngine

/**
 * Describes an interruption to be performed.
 *
 * @param exitAction
 *   the action to execute next, instead of following the regular workflow.
 * @param session
 *   the new Session to be sent to exitAction
 * @param groupsToClose
 *   the groups to be closed as we bypass the regular GroupEnd from the regular flow
 */
private final case class BlockExit(exitAction: Action, session: Session, groupsToClose: List[GroupBlock]) {
  def exitBlock(statsEngine: StatsEngine, nowMillis: Long): Unit = {
    groupsToClose.reverseIterator.foreach(statsEngine.logGroupEnd(session.scenario, _, nowMillis))
    exitAction ! session
  }
}

private object BlockExit {
  private def blockExit(blocks: List[Block], until: Block, exitAction: Action, session: Session): BlockExit = {
    @tailrec
    def blockExitRec(blocks: List[Block], session: Session, groupsToClose: List[GroupBlock]): BlockExit = blocks match {
      case head :: tail =>
        head match {
          case `until`                             => BlockExit(exitAction, session, groupsToClose)
          case group: GroupBlock                   => blockExitRec(tail, session.exitGroup(tail), group :: groupsToClose)
          case TryMaxBlock(counterName, _, status) => blockExitRec(tail, session.exitTryMax(counterName, status, tail), groupsToClose)
          case loopBlock: LoopBlock                => blockExitRec(tail, session.exitLoop(loopBlock.counterName, tail), groupsToClose)
        }
      case _ => BlockExit(exitAction, session, groupsToClose)
    }

    blockExitRec(blocks, session, Nil)
  }

  /**
   * Scan the block stack for ExitAsap loops. Scan is performed from right to left (right is deeper) = normal.
   *
   * @param session
   *   the session
   * @return
   *   the potential Interruption to process
   */
  private def exitAsapLoop(session: Session): Option[BlockExit] = {
    @tailrec
    def exitAsapLoopRec(leftToRightBlocks: List[Block], rightToLeftBlocks: List[Block]): Option[BlockExit] = leftToRightBlocks match {
      case head :: tail =>
        head match {
          case ExitAsapLoopBlock(counterName, condition, exitAction) if !LoopBlock.continue(condition, session) =>
            val exit = blockExit(session.blockStack, head, exitAction, session)
            // block stack head is now the loop itself, we must exit it
            Some(exit.copy(session = exit.session.exitLoop(counterName, rightToLeftBlocks)))

          case _ => exitAsapLoopRec(tail, head :: rightToLeftBlocks)
        }
      case _ => None
    }

    exitAsapLoopRec(session.blockStack.reverse, Nil)
  }

  /**
   * Scan the block stack for TryMax loops. Scan is performed from right to left (right is deeper) = normal.
   *
   * @param session
   *   the session
   * @return
   *   the potential Interruption to process
   */
  private def exitTryMax(session: Session): Option[BlockExit] = {
    @tailrec
    def exitTryMaxRec(stack: List[Block]): Option[BlockExit] = stack match {
      case head :: tail =>
        head match {
          case TryMaxBlock(_, tryMaxActor, KO) =>
            // block stack head is now the tryMax itself, leave as is
            Some(blockExit(session.blockStack, head, tryMaxActor, session))

          case _ => exitTryMaxRec(tail)
        }
      case _ => None
    }

    exitTryMaxRec(session.blockStack)
  }

  def mustExit(session: Session): Option[BlockExit] =
    exitAsapLoop(session).orElse(exitTryMax(session))
}
