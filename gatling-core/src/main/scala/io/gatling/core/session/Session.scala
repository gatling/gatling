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

package io.gatling.core.session

import scala.annotation.tailrec
import scala.reflect.ClassTag

import io.gatling.commons.NotNothing
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.TypeCaster
import io.gatling.commons.util.TypeHelper
import io.gatling.commons.validation._
import io.gatling.core.action.Action
import io.gatling.core.session.el.ElMessages
import io.gatling.core.stats.message.ResponseTimings

import com.typesafe.scalalogging.LazyLogging
import io.netty.channel.EventLoop

object SessionPrivateAttributes {

  val PrivateAttributePrefix = "gatling."
}

final case class SessionAttribute(session: Session, key: String) {

  def as[T: TypeCaster: ClassTag: NotNothing]: T = session.attributes.get(key) match {
    case Some(value) => TypeHelper.cast[T](key, value)
    case _           => throw new NoSuchElementException(ElMessages.undefinedSessionAttribute(key).message)
  }
  def asOption[T: TypeCaster: ClassTag: NotNothing]: Option[T] = session.attributes.get(key).map(TypeHelper.cast[T](key, _))
  def validate[T: TypeCaster: ClassTag: NotNothing]: Validation[T] = session.attributes.get(key) match {
    case Some(value) => TypeHelper.validate[T](key, value)
    case _           => ElMessages.undefinedSessionAttribute(key)
  }
}

object Session {
  val MarkAsFailedUpdate: Session => Session = _.markAsFailed
  val Identity: Session => Session = identity
  val NothingOnExit: Session => Unit = _ => ()

  private[session] def timestampName(counterName: String) = "timestamp." + counterName

  def apply(
      scenario: String,
      userId: Long,
      eventLoop: EventLoop
  ): Session =
    apply(scenario, userId, Session.NothingOnExit, eventLoop)

  private[core] def apply(
      scenario: String,
      userId: Long,
      onExit: Session => Unit,
      eventLoop: EventLoop
  ): Session =
    Session(
      scenario = scenario,
      userId = userId,
      attributes = Map.empty,
      baseStatus = OK,
      blockStack = Nil,
      onExit = onExit,
      eventLoop: EventLoop
    )
}

/**
 * Session class representing the session passing through a scenario for a given user
 *
 * This session stores all needed data between requests
 *
 * @constructor creates a new session
 * @param scenario the name of the current scenario
 * @param userId the id of the current user
 * @param attributes the map that stores all values needed
 * @param baseStatus the status when not in a TryMax blocks hierarchy
 * @param blockStack the block stack
 * @param onExit hook to execute once the user reaches the exit
 */
final case class Session(
    scenario: String,
    userId: Long,
    attributes: Map[String, Any],
    baseStatus: Status,
    blockStack: List[Block],
    onExit: Session => Unit,
    eventLoop: EventLoop
) extends LazyLogging {

  import Session._

  def apply(name: String): SessionAttribute = SessionAttribute(this, name)
  def setAll(newAttributes: (String, Any)*): Session = setAll(newAttributes.toIterable)
  def setAll(newAttributes: Iterable[(String, Any)]): Session = copy(attributes = attributes ++ newAttributes)
  def set(key: String, value: Any): Session = copy(attributes = attributes + (key -> value))
  def remove(key: String): Session = if (contains(key)) copy(attributes = attributes - key) else this
  def removeAll(keys: String*): Session = keys.foldLeft(this)(_ remove _)
  def contains(attributeKey: String): Boolean = attributes.contains(attributeKey)

  def reset: Session = {
    val newAttributes =
      if (blockStack.isEmpty) {
        // not in a block
        attributes.view.filterKeys(_.startsWith(SessionPrivateAttributes.PrivateAttributePrefix))
      } else {
        val counterNames: Set[String] = blockStack.view.collect { case counterBlock: CounterBlock => counterBlock.counterName }.to(Set)
        if (counterNames.isEmpty) {
          // no counter based blocks (only groups)
          attributes.view.filterKeys(_.startsWith(SessionPrivateAttributes.PrivateAttributePrefix))
        } else {
          val timestampNames: Set[String] = counterNames.map(timestampName)
          attributes.view.filterKeys(key =>
            counterNames.contains(key) || timestampNames.contains(key) || key.startsWith(SessionPrivateAttributes.PrivateAttributePrefix)
          )
        }
      }
    copy(attributes = newAttributes.to(Map))
  }

  def loopCounterValue(counterName: String): Int = attributes(counterName).asInstanceOf[Int]

  def loopTimestampValue(counterName: String): Long = attributes(timestampName(counterName)).asInstanceOf[Long]

  @SuppressWarnings(Array("org.wartremover.warts.ListAppend"))
  private[gatling] def enterGroup(groupName: String, nowMillis: Long): Session = {
    val groups = blockStack.collectFirst { case g: GroupBlock => g.groups } match {
      case Some(l) => l :+ groupName
      case _       => groupName :: Nil
    }
    copy(blockStack = GroupBlock(groups, nowMillis, 0, OK) :: blockStack)
  }

  private[core] def exitGroup(tail: List[Block]): Session = copy(blockStack = tail)

  def logGroupRequestTimings(startTimestamp: Long, endTimestamp: Long): Session =
    if (blockStack.isEmpty) {
      this
    } else {
      val responseTime = ResponseTimings.responseTime(startTimestamp, endTimestamp)
      copy(blockStack = blockStack.map {
        case g: GroupBlock => g.copy(cumulatedResponseTime = g.cumulatedResponseTime + responseTime)
        case b             => b
      })
    }

  def groups: List[String] = {

    @tailrec
    def gh(blocks: List[Block]): List[String] = blocks match {
      case head :: tail =>
        head match {
          case g: GroupBlock => g.groups
          case _             => gh(tail)
        }
      case _ => Nil
    }

    gh(blockStack)
  }

  private[core] def enterTryMax(counterName: String, loopAction: Action) =
    copy(
      blockStack = TryMaxBlock(counterName, loopAction, OK) :: blockStack,
      attributes = newAttributesWithCounter(counterName, withTimestamp = false, 0L)
    )

  private[core] def exitTryMax: Session = blockStack match {
    case TryMaxBlock(counterName, _, status) :: tail =>
      copy(blockStack = tail).updateStatus(status).removeCounter(counterName)

    case _ =>
      logger.error(s"exitTryMax called but stack head $blockStack isn't a TryMaxBlock, please report.")
      this
  }

  def isFailed: Boolean = baseStatus == KO || blockStack.exists {
    case TryMaxBlock(_, _, KO) => true
    case _                     => false
  }

  def status: Status = if (isFailed) KO else OK

  private def failStatusUntilClosestTryMaxBlock: List[Block] = {
    var firstTryMaxBlockNotReached = true
    blockStack.map {
      case tryMaxBlock: TryMaxBlock if firstTryMaxBlockNotReached && tryMaxBlock.status == OK =>
        firstTryMaxBlockNotReached = false
        tryMaxBlock.copy(status = KO)
      case groupBlock: GroupBlock if firstTryMaxBlockNotReached && groupBlock.status == OK =>
        groupBlock.copy(status = KO)
      case b => b
    }
  }

  private def restoreClosestTryMaxBlockStatus: List[Block] = {
    var firstTryMaxBlockNotReached = true
    blockStack.map {
      case tryMaxBlock: TryMaxBlock if firstTryMaxBlockNotReached && tryMaxBlock.status == KO =>
        firstTryMaxBlockNotReached = false
        tryMaxBlock.copy(status = OK)
      case b => b
    }
  }

  private def updateStatus(newStatus: Status): Session =
    if (newStatus == OK) {
      markAsSucceeded
    } else {
      markAsFailed
    }

  private def isWithinTryMax: Boolean = blockStack.exists(_.isInstanceOf[TryMaxBlock])

  def markAsSucceeded: Session =
    if (isWithinTryMax) {
      copy(blockStack = restoreClosestTryMaxBlockStatus)
    } else if (baseStatus == KO) {
      copy(baseStatus = OK)
    } else {
      this
    }

  def markAsFailed: Session =
    if (baseStatus == KO && blockStack.isEmpty) {
      this
    } else {
      val updatedStatus = if (isWithinTryMax) baseStatus else KO
      copy(baseStatus = updatedStatus, blockStack = failStatusUntilClosestTryMaxBlock)
    }

  private def newBlockStack(counterName: String, condition: Expression[Boolean], exitAction: Action, exitASAP: Boolean): List[Block] = {
    val newBlock =
      if (exitASAP) {
        ExitAsapLoopBlock(counterName, condition, exitAction)
      } else {
        ExitOnCompleteLoopBlock(counterName)
      }
    newBlock :: blockStack
  }

  private[core] def enterLoop(counterName: String, condition: Expression[Boolean], exitAction: Action, exitASAP: Boolean): Session =
    copy(
      blockStack = newBlockStack(counterName, condition, exitAction, exitASAP),
      attributes = newAttributesWithCounter(counterName, withTimestamp = false, 0L)
    )

  private[core] def enterTimeBasedLoop(
      counterName: String,
      condition: Expression[Boolean],
      exitAction: Action,
      exitASAP: Boolean,
      nowMillis: Long
  ): Session =
    copy(
      blockStack = newBlockStack(counterName, condition, exitAction, exitASAP),
      attributes = newAttributesWithCounter(counterName, withTimestamp = true, nowMillis)
    )

  private[core] def exitLoop: Session = blockStack match {
    case LoopBlock(counterName) :: tail => copy(blockStack = tail).removeCounter(counterName)
    case _ =>
      logger.error(s"exitLoop called but stack head $blockStack isn't a Loop Block, please report.")
      this
  }

  private def newAttributesWithCounter(counterName: String, withTimestamp: Boolean, nowMillis: Long): Map[String, Any] = {
    val withCounter = attributes.updated(counterName, 0)
    if (withTimestamp) {
      withCounter.updated(timestampName(counterName), nowMillis)
    } else {
      withCounter
    }
  }

  private[core] def incrementCounter(counterName: String): Session =
    attributes.get(counterName) match {
      case Some(counterValue: Int) => copy(attributes = attributes.updated(counterName, counterValue + 1))
      case _ =>
        logger.error(s"incrementCounter called but attribute for counterName $counterName is missing, please report.")
        this
    }

  private[core] def removeCounter(counterName: String): Session =
    attributes.get(counterName) match {
      case None =>
        logger.error(s"removeCounter called but attribute for counterName $counterName is missing, please report.")
        this
      case _ =>
        copy(attributes = attributes - counterName - timestampName(counterName))
    }

  private[core] def exit(): Unit = onExit(this)
}
