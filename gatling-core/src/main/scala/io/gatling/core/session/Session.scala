/*
 * Copyright 2011-2018 GatlingCorp (https://gatling.io)
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
import scala.collection.breakOut
import scala.reflect.ClassTag

import io.gatling.commons.NotNothing
import io.gatling.commons.stats.{ KO, OK, Status }
import io.gatling.commons.util.TypeCaster
import io.gatling.commons.util.TypeHelper._
import io.gatling.commons.validation._
import io.gatling.core.action.Action
import io.gatling.core.session.el.ElMessages
import io.gatling.core.stats.message.ResponseTimings

import com.typesafe.scalalogging.LazyLogging

/**
 * Private Gatling Session attributes
 */
object SessionPrivateAttributes {

  val PrivateAttributePrefix = "gatling."
}

case class SessionAttribute(session: Session, key: String) {

  def as[T: TypeCaster: ClassTag: NotNothing]: T = session.attributes.get(key) match {
    case Some(value) => value.as[T]
    case _           => throw new NoSuchElementException(ElMessages.undefinedSessionAttribute(key).message)
  }
  def asOption[T: TypeCaster: ClassTag: NotNothing]: Option[T] = session.attributes.get(key).flatMap(_.asOption[T])
  def validate[T: TypeCaster: ClassTag: NotNothing]: Validation[T] = session.attributes.get(key) match {
    case Some(value) => value.asValidation[T]
    case _           => ElMessages.undefinedSessionAttribute(key)
  }
}

object Session {
  val MarkAsFailedUpdate: Session => Session = _.markAsFailed
  val Identity: Session => Session = identity[Session]
  val NothingOnExit: Session => Unit = _ => ()

  private[session] def timestampName(counterName: String) = "timestamp." + counterName
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
 * @param startDate when the user was started
 * @param drift the cumulated time that was spent in Gatling on computation and that wasn't compensated for
 * @param baseStatus the status when not in a TryMax blocks hierarchy
 * @param blockStack the block stack
 * @param onExit hook to execute once the user reaches the exit
 */
case class Session(
    scenario:   String,
    userId:     Long,
    startDate:  Long,
    attributes: Map[String, Any] = Map.empty,
    drift:      Long             = 0L,
    baseStatus: Status           = OK,
    blockStack: List[Block]      = Nil,
    onExit:     Session => Unit  = Session.NothingOnExit
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
        Map.empty[String, Any]
      } else {
        val counterNames: Set[String] = blockStack.collect { case counterBlock: CounterBlock => counterBlock.counterName }(breakOut)
        if (counterNames.isEmpty) {
          // no counter based blocks (only groups)
          Map.empty[String, Any]
        } else {
          val timestampNames: Set[String] = counterNames.map(timestampName)
          attributes.filter { case (key, _) => counterNames.contains(key) || timestampNames.contains(key) || key.startsWith(SessionPrivateAttributes.PrivateAttributePrefix) }
        }
      }
    copy(attributes = newAttributes)
  }

  private[gatling] def setDrift(drift: Long) = copy(drift = drift)
  private[gatling] def increaseDrift(time: Long) = copy(drift = time + drift)

  def loopCounterValue(counterName: String): Int = attributes(counterName).asInstanceOf[Int]

  def loopTimestampValue(counterName: String): Long = attributes(timestampName(counterName)).asInstanceOf[Long]

  private[gatling] def enterGroup(groupName: String, nowMillis: Long) = {
    val groupHierarchy = blockStack.collectFirst { case g: GroupBlock => g.hierarchy } match {
      case None    => List(groupName)
      case Some(l) => l :+ groupName
    }
    copy(blockStack = GroupBlock(groupHierarchy, nowMillis) :: blockStack)
  }

  private[gatling] def exitGroup = blockStack match {
    case head :: tail if head.isInstanceOf[GroupBlock] => copy(blockStack = tail)
    case _ =>
      logger.error(s"exitGroup called but stack head $blockStack isn't a GroupBlock, please report.")
      this
  }

  private[gatling] def logGroupRequest(startTimestamp: Long, endTimestamp: Long, status: Status) = blockStack match {
    case Nil => this
    case _ =>
      val responseTime = ResponseTimings.responseTime(startTimestamp, endTimestamp)
      copy(blockStack = blockStack.map {
        case g: GroupBlock => g.copy(cumulatedResponseTime = g.cumulatedResponseTime + responseTime, status = if (status == KO) KO else g.status)
        case b             => b
      })
  }

  def groupHierarchy: List[String] = {

    @tailrec
    def gh(blocks: List[Block]): List[String] = blocks match {
      case Nil => Nil
      case head :: tail => head match {
        case g: GroupBlock => g.hierarchy
        case _             => gh(tail)
      }
    }

    gh(blockStack)
  }

  private[gatling] def enterTryMax(counterName: String, loopAction: Action) =
    copy(
      blockStack = TryMaxBlock(counterName, loopAction) :: blockStack,
      attributes = newAttributesWithCounter(counterName, withTimestamp = false, 0L)
    )

  private[gatling] def exitTryMax: Session = blockStack match {
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

  private def failStatusUntilFirstTryMaxBlock: List[Block] = {
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

  private def restoreFirstTryMaxBlockStatus: List[Block] = {
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
      copy(blockStack = restoreFirstTryMaxBlockStatus)
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
      copy(baseStatus = updatedStatus, blockStack = failStatusUntilFirstTryMaxBlock)
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

  private[gatling] def enterLoop(counterName: String, condition: Expression[Boolean], exitAction: Action, exitASAP: Boolean): Session =
    copy(
      blockStack = newBlockStack(counterName, condition, exitAction, exitASAP),
      attributes = newAttributesWithCounter(counterName, withTimestamp = false, 0L)
    )

  private[gatling] def enterTimeBasedLoop(counterName: String, condition: Expression[Boolean], exitAction: Action, exitASAP: Boolean, nowMillis: Long): Session =
    copy(
      blockStack = newBlockStack(counterName, condition, exitAction, exitASAP),
      attributes = newAttributesWithCounter(counterName, withTimestamp = true, nowMillis)
    )

  private[gatling] def exitLoop: Session = blockStack match {
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

  private[gatling] def incrementCounter(counterName: String): Session =
    attributes.get(counterName) match {
      case Some(counterValue: Int) => copy(attributes = attributes.updated(counterName, counterValue + 1))
      case _ =>
        logger.error(s"incrementCounter called but attribute for counterName $counterName is missing, please report.")
        this
    }

  private[gatling] def removeCounter(counterName: String): Session =
    attributes.get(counterName) match {
      case None =>
        logger.error(s"removeCounter called but attribute for counterName $counterName is missing, please report.")
        this
      case _ =>
        copy(attributes = attributes - counterName - timestampName(counterName))
    }

  def update(updates: Iterable[Session => Session]): Session = updates.foldLeft(this) {
    (session, update) => update(session)
  }

  def exit(): Unit = onExit(this)
}
