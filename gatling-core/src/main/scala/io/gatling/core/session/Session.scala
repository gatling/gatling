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

import com.typesafe.scalalogging.slf4j.StrictLogging

import io.gatling.core.NotNothing
import io.gatling.core.result.message.{ KO, OK, Status }
import io.gatling.core.session.el.ELMessages
import io.gatling.core.util.TimeHelper.nowMillis
import io.gatling.core.util.TypeHelper.TypeCaster
import io.gatling.core.validation.{ FailureWrapper, Validation }
import akka.actor.ActorRef

/**
 * Private Gatling Session attributes
 */
object SessionPrivateAttributes {

  val privateAttributePrefix = "gatling."
}

case class SessionAttribute(session: Session, key: String) {

  def as[T: NotNothing]: T = session.attributes(key).asInstanceOf[T]
  def asOption[T: NotNothing]: Option[T] = session.attributes.get(key).map(_.asInstanceOf[T])
  def validate[T](implicit ct: ClassTag[T], nn: NotNothing[T]): Validation[T] = session.attributes.get(key) match {
    case Some(value) => value.asValidation[T]
    case None        => ELMessages.undefinedSessionAttributeMessage(key).failure
  }
}

object Session {
  val MarkAsFailedUpdate: Session => Session = _.markAsFailed
}

/**
 * Session class representing the session passing through a scenario for a given user
 *
 * This session stores all needed data between requests
 *
 * @constructor creates a new session
 * @param scenarioName the name of the current scenario
 * @param userId the id of the current user
 * @param attributes the map that stores all values needed
 * @param startDate when the user was started
 * @param drift the cumulated time that was spent in Gatling on computation and that wasn't compensated for
 * @param baseStatus the status when not in a TryMax blocks hierarchy
 * @param blockStack the block stack
 */
case class Session(
    scenarioName: String,
    userId: String,
    attributes: Map[String, Any] = Map.empty,
    startDate: Long = nowMillis,
    drift: Long = 0L,
    baseStatus: Status = OK,
    blockStack: List[Block] = Nil) extends StrictLogging {

  def apply(name: String) = SessionAttribute(this, name)
  def setAll(newAttributes: (String, Any)*): Session = setAll(newAttributes.toIterable)
  def setAll(newAttributes: Iterable[(String, Any)]): Session = copy(attributes = attributes ++ newAttributes)
  def set(key: String, value: Any) = copy(attributes = attributes + (key -> value))
  def remove(key: String) = if (contains(key)) copy(attributes = attributes - key) else this
  def removeAll(keys: String*) = copy(attributes = attributes -- keys)
  def contains(attributeKey: String) = attributes.contains(attributeKey)

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

  private[gatling] def logGroupAsyncRequests(asyncFetchTime: Long, okCount: Int, koCount: Int) = blockStack match {
    case Nil => this
    case _ =>
      copy(blockStack = blockStack.map {
        case g: GroupBlock => g.copy(cumulatedResponseTime = g.cumulatedResponseTime + asyncFetchTime, oks = g.oks + okCount, kos = g.kos + koCount)
        case b             => b
      })
  }

  private[gatling] def logGroupRequest(responseTime: Long, status: Status) = blockStack match {
    case Nil => this
    case _ =>
      val (ok, ko) = if (status == OK) (1, 0) else (0, 1)
      copy(blockStack = blockStack.map {
        case g: GroupBlock => g.copy(cumulatedResponseTime = g.cumulatedResponseTime + responseTime, oks = g.oks + ok, kos = g.kos + ko)
        case b             => b
      })
  }

  def groupHierarchy: List[String] = blockStack.collectFirst { case g: GroupBlock => g.hierarchy }.getOrElse(Nil)

  private[gatling] def enterTryMax(counterName: String, loopActor: ActorRef) =
    copy(blockStack = TryMaxBlock(counterName, loopActor) :: blockStack).initCounter(counterName)

  private[gatling] def exitTryMax: Session = blockStack match {
    case TryMaxBlock(counterName, _, status) :: tail =>
      val newStack =
        if (status == KO) {
          // propagate failure to closest TryMax
          var first = true
          tail.map {
            case tryMax: TryMaxBlock if first =>
              first = false
              tryMax.copy(status = KO)
            case b => b
          }
        } else
          tail

      copy(blockStack = newStack).removeCounter(counterName)

    case _ =>
      logger.error(s"exitTryMax called but stack head $blockStack isn't a TryMaxBlock, please report.")
      this
  }

  def isFailed = baseStatus == KO || blockStack.exists {
    case TryMaxBlock(_, _, KO) => true
    case _                     => false
  }

  def status: Status = if (isFailed) KO else OK

  def markAsSucceeded: Session = {
    var first = true
    val newStack = blockStack.map {
      case tryMax: TryMaxBlock if first =>
        first = false
        tryMax.copy(status = OK)
      case b => b
    }

    if (first) {
      if (baseStatus == OK)
        this
      else
        copy(baseStatus = OK)
    } else
      copy(blockStack = newStack)
  }

  def markAsFailed: Session = {
    var first = true
    val newStack = blockStack.map {
      case tryMax: TryMaxBlock if first =>
        first = false
        tryMax.copy(status = KO)
      case b => b
    }

    if (first) {
      if (baseStatus == KO)
        this
      else
        copy(baseStatus = KO)
    } else
      copy(blockStack = newStack)
  }

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
}
