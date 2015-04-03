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
package io.gatling.core.session

import scala.reflect.ClassTag

import com.typesafe.scalalogging.LazyLogging

import io.gatling.core.NotNothing
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.session.el.ElMessages
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.util.TypeHelper.TypeCaster
import io.gatling.core.validation.Validation
import akka.actor.ActorRef

/**
 * Private Gatling Session attributes
 */
object SessionPrivateAttributes {

  val PrivateAttributePrefix = "gatling."
}

case class SessionAttribute(session: Session, key: String) {

  def as[T: NotNothing]: T = session.attributes(key).asInstanceOf[T]
  def asOption[T: NotNothing]: Option[T] = session.attributes.get(key).map(_.asInstanceOf[T])
  def validate[T: ClassTag: NotNothing]: Validation[T] = session.attributes.get(key) match {
    case Some(value) => value.asValidation[T]
    case None        => ElMessages.undefinedSessionAttribute(key)
  }
}

object Session {
  val MarkAsFailedUpdate: Session => Session = _.markAsFailed
  val Identity: Session => Session = identity[Session]
}

/**
 * Session class representing the session passing through a scenario for a given user
 *
 * This session stores all needed data between requests
 *
 * @constructor creates a new session
 * @param scenario the name of the current scenario
 * @param userId the id of the current user
 * @param userEnd hook to execute once the user terminates
 * @param attributes the map that stores all values needed
 * @param startDate when the user was started
 * @param drift the cumulated time that was spent in Gatling on computation and that wasn't compensated for
 * @param baseStatus the status when not in a TryMax blocks hierarchy
 * @param blockStack the block stack
 */
case class Session(
    scenario: String,
    userId: String,
    attributes: Map[String, Any] = Map.empty,
    startDate: Long = nowMillis,
    drift: Long = 0L,
    baseStatus: Status = OK,
    blockStack: List[Block] = Nil,
    userEnd: Session => Unit = session => ()) extends LazyLogging {

  def apply(name: String) = SessionAttribute(this, name)
  def setAll(newAttributes: (String, Any)*): Session = setAll(newAttributes.toIterable)
  def setAll(newAttributes: Iterable[(String, Any)]): Session = copy(attributes = attributes ++ newAttributes)
  def set(key: String, value: Any) = copy(attributes = attributes + (key -> value))
  def remove(key: String) = if (contains(key)) copy(attributes = attributes - key) else this
  def removeAll(keys: String*) = keys.foldLeft(this)(_ remove _)
  def contains(attributeKey: String) = attributes.contains(attributeKey)
  def reset = copy(attributes = Map.empty)

  private[gatling] def setDrift(drift: Long) = copy(drift = drift)
  private[gatling] def increaseDrift(time: Long) = copy(drift = time + drift)

  private def timestampName(counterName: String) = "timestamp." + counterName

  def loopCounterValue(counterName: String) = attributes(counterName).asInstanceOf[Int]

  def loopTimestampValue(counterName: String) = attributes(timestampName(counterName)).asInstanceOf[Long]

  private[gatling] def enterGroup(groupName: String) = {
    val groupHierarchy = blockStack.collectFirst { case g: GroupBlock => g.hierarchy } match {
      case None    => List(groupName)
      case Some(l) => l :+ groupName
    }
    copy(blockStack = GroupBlock(groupHierarchy) :: blockStack)
  }

  private[gatling] def exitGroup = blockStack match {
    case head :: tail if head.isInstanceOf[GroupBlock] => copy(blockStack = tail)
    case _ =>
      logger.error(s"exitGroup called but stack head $blockStack isn't a GroupBlock, please report.")
      this
  }

  private[gatling] def logGroupRequest(responseTime: Int, status: Status) = blockStack match {
    case Nil => this
    case _ =>
      copy(blockStack = blockStack.map {
        case g: GroupBlock => g.copy(cumulatedResponseTime = g.cumulatedResponseTime + responseTime, status = if (status == KO) KO else g.status)
        case b             => b
      })
  }

  def groupHierarchy: List[String] = blockStack.collectFirst { case g: GroupBlock => g.hierarchy }.getOrElse(Nil)

  private[gatling] def enterTryMax(counterName: String, loopActor: ActorRef) =
    copy(blockStack = TryMaxBlock(counterName, loopActor) :: blockStack).initCounter(counterName)

  private[gatling] def exitTryMax: Session = blockStack match {
    case TryMaxBlock(counterName, _, status) :: tail =>
      copy(blockStack = tail).updateStatus(status).removeCounter(counterName)

    case _ =>
      logger.error(s"exitTryMax called but stack head $blockStack isn't a TryMaxBlock, please report.")
      this
  }

  def isFailed = baseStatus == KO || blockStack.exists {
    case TryMaxBlock(_, _, KO) => true
    case _                     => false
  }

  def status: Status = if (isFailed) KO else OK

  private def updateStatus(newStatus: Status): Session = {

      def isInTryMax = blockStack.exists(_.isInstanceOf[TryMaxBlock])

      def changeFirstTryMaxStatus(oldStatus: Status, newStatus: Status): List[Block] = {
        var first = true
        blockStack.map {
          case tryMax: TryMaxBlock if first =>
            first = false
            if (tryMax.status == oldStatus) tryMax.copy(status = newStatus)
            else tryMax
          case b => b
        }
      }

    val oldStatus = if (newStatus == OK) KO else OK

    if (!isInTryMax) {
      if (baseStatus == oldStatus) copy(baseStatus = newStatus)
      else this

    } else {
      copy(blockStack = changeFirstTryMaxStatus(oldStatus, newStatus))
    }
  }

  def markAsSucceeded: Session = updateStatus(OK)

  def markAsFailed: Session = updateStatus(KO)

  private[gatling] def enterLoop(counterName: String, condition: Expression[Boolean], loopActor: ActorRef, exitASAP: Boolean): Session = {

    val newBlock =
      if (exitASAP)
        ExitASAPLoopBlock(counterName, condition, loopActor)
      else
        ExitOnCompleteLoopBlock(counterName)

    copy(blockStack = newBlock :: blockStack).initCounter(counterName)
  }

  private[gatling] def exitLoop: Session = blockStack match {
    case LoopBlock(counterName) :: tail => copy(blockStack = tail).removeCounter(counterName)
    case _ =>
      logger.error(s"exitLoop called but stack head $blockStack isn't a Loop Block, please report.")
      this
  }

  private[gatling] def initCounter(counterName: String): Session =
    copy(attributes = attributes + (counterName -> 0) + (timestampName(counterName) -> nowMillis))

  private[gatling] def incrementCounter(counterName: String): Session =
    attributes.get(counterName) match {
      case Some(counterValue: Int) => copy(attributes = attributes + (counterName -> (counterValue + 1)))
      case _ =>
        logger.error(s"incrementCounter called but attribute for counterName $counterName is missing, please report.")
        this
    }

  private[gatling] def removeCounter(counterName: String): Session =
    attributes.get(counterName) match {
      case Some(counterValue: Int) => copy(attributes = attributes - counterName - timestampName(counterName))
      case _ =>
        logger.error(s"removeCounter called but attribute for counterName $counterName is missing, please report.")
        this
    }

  def update(updates: Iterable[Session => Session]): Session = updates.foldLeft(this) {
    (session, update) => update(session)
  }

  def terminate(): Unit = userEnd(this)
}
